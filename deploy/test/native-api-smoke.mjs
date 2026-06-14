#!/usr/bin/env node
/**
 * Native Docker API 冒烟测试（L1–L4）
 * 用法: node deploy/test/native-api-smoke.mjs [baseUrl]
 */
const BASE = process.argv[2] || 'http://127.0.0.1:2020';
const NATIVE_ERROR_PATTERNS = [
  'MissingReflectionRegistrationError',
  'NoClassDefFoundError',
  'ExceptionInInitializerError',
  'Could not initialize class',
  'Executor.query',
  'SSLMSW',
  'json_contains',
  'JSON_CONTAINS',
  'date_format',
  'DATE_FORMAT',
  'Function "',
  'JdbcSQLSyntaxErrorException',
];

const results = [];
let token = '';

async function req(method, path, { body, headers = {}, multipart } = {}) {
  const url = `${BASE}${path}`;
  const opts = { method, headers: { ...headers } };
  if (token) {
    opts.headers.Authorization = `Bearer ${token}`;
  }
  if (multipart) {
    opts.body = multipart;
  } else if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(url, opts);
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    json = { raw: text.slice(0, 500) };
  }
  return { status: res.status, json, text };
}

function assertOk(name, res, expectCode = 200) {
  const msg = res.json?.msg || res.text || '';
  const nativeErr = NATIVE_ERROR_PATTERNS.find((p) => String(msg).includes(p));
  const codeOk = res.json?.code === expectCode || res.status === expectCode;
  const pass = codeOk && !nativeErr;
  results.push({ name, pass, code: res.json?.code, status: res.status, msg: String(msg).slice(0, 120) });
  if (!pass) {
    console.error(`FAIL ${name}: code=${res.json?.code} status=${res.status} msg=${msg}`);
  } else {
    console.log(`OK   ${name}`);
  }
  return pass;
}

async function main() {
  console.log(`==> Native API smoke @ ${BASE}\n`);

  let r = await req('GET', '/version');
  assertOk('L1 GET /version', r);

  r = await req('GET', '/captchaImage');
  assertOk('L1 GET /captchaImage', r);

  r = await req('GET', '/setup/status');
  assertOk('L1 GET /setup/status', r);

  r = await req('POST', '/login', {
    body: { username: 'demo', password: '123456', code: '', uuid: '' },
  });
  assertOk('L2 POST /login', r);
  token = r.json?.token;
  if (!token) {
    console.error('No token, aborting authenticated tests');
    summarize();
    process.exit(1);
  }

  r = await req('GET', '/getInfo');
  assertOk('L2 GET /getInfo', r);

  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64'
  );
  const form = new FormData();
  form.append('file', new Blob([png], { type: 'image/png' }), 'smoke.png');
  r = await req('POST', '/common/upload', { multipart: form });
  assertOk('L3 POST /common/upload', r);
  const iconUrl = r.json?.url || r.json?.fileName || '/profile/upload/smoke.png';

  r = await req('POST', '/system/team', {
    body: {
      teamName: `smoke-${Date.now()}`,
      teamIcon: iconUrl,
      remark: 'native smoke',
    },
  });
  assertOk('L3 POST /system/team (create)', r);

  r = await req('GET', '/system/team/my');
  assertOk('L3 GET /system/team/my', r);

  const samples = [
    ['L4 GET /system/team/list', 'GET', '/system/team/list?pageNum=1&pageSize=10'],
    ['L4 GET /system/project/list', 'GET', '/system/project/list?pageNum=1&pageSize=10'],
    ['L4 GET /system/defect/list', 'GET', '/system/defect/list?pageNum=1&pageSize=10'],
    ['L4 GET /system/case/list', 'GET', '/system/case/list?pageNum=1&pageSize=10'],
    ['L4 GET /system/member/list', 'GET', '/system/member/list?pageNum=1&pageSize=10'],
    ['L4 GET /admin/team/list', 'GET', '/admin/team/list?pageNum=1&pageSize=10'],
  ];
  for (const [name, method, path] of samples) {
    r = await req(method, path);
    const msg = r.json?.msg || '';
    const nativeErr = NATIVE_ERROR_PATTERNS.find((p) => String(msg).includes(p));
    const pass = !nativeErr && r.json?.code !== 500;
    results.push({ name, pass, code: r.json?.code, status: r.status, msg: String(msg).slice(0, 120) });
    console.log(pass ? `OK   ${name}` : `FAIL ${name}: ${msg}`);
  }

  let projectId = 1;
  r = await req('GET', '/system/project/list?pageNum=1&pageSize=1');
  if (r.json?.rows?.[0]?.projectId) {
    projectId = r.json.rows[0].projectId;
  }

  const h2SqlSamples = [
    ['L5 GET open-workload', 'GET', `/system/defect/statistic/open-workload/${projectId}?pageNum=1&pageSize=10`],
    ['L5 GET open-workload/my', 'GET', `/system/defect/statistic/open-workload/${projectId}/my`],
    ['L5 GET defect-state-line', 'GET', `/system/dashboard/${projectId}/defect-line?timeType=day`],
    ['L5 GET member-defect-line', 'GET', `/system/dashboard/${projectId}/member-defect-line?timeType=day`],
    ['L5 GET member-defect rank', 'GET', `/system/dashboard/${projectId}/member-defect`],
    ['L5 GET state statistic', 'GET', `/system/defect/statistic/state/${projectId}`],
  ];
  for (const [name, method, path] of h2SqlSamples) {
    r = await req(method, path);
    assertOk(name, r);
  }

  summarize();
  process.exit(results.every((x) => x.pass) ? 0 : 1);
}

function summarize() {
  const passed = results.filter((x) => x.pass).length;
  console.log(`\n==> ${passed}/${results.length} passed`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
