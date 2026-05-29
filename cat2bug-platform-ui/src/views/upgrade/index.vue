<template>
  <div
    class="setup-page"
    :class="{ 'setup-page--rtl': isRtl }"
    :dir="isRtl ? 'rtl' : 'ltr'"
  >
    <div class="setup-page-watermark" aria-hidden="true">
      <div class="setup-page-watermark__pattern" />
    </div>
    <div class="setup-layout">
      <header class="setup-hero">
        <div class="setup-hero-bg" aria-hidden="true" />
        <div class="setup-hero-head">
          <h1>{{ $t('upgrade.title') }}</h1>
          <p class="setup-intro-lead">{{ $t('upgrade.introduce1') }}</p>
        </div>
        <div class="setup-hero-art" aria-hidden="true">
          <img
            class="setup-hero-art-img"
            :src="brandLogoSrc"
            alt=""
          >
        </div>
      </header>

      <div class="setup-body">
        <aside class="setup-aside">
          <div class="setup-aside-content">
            <h2 class="setup-roadmap-title">{{ $t('upgrade.roadmapTitle') }}</h2>
            <ol class="setup-roadmap">
              <li
                v-for="(key, index) in stepKeys"
                :key="key"
                class="setup-roadmap-item"
                :class="{
                  'is-active': activeStep === index,
                  'is-done': activeStep > index || (activeStep === 4 && isSuccessResult)
                }"
              >
                <div class="setup-roadmap-marker-col">
                  <span class="setup-roadmap-marker">
                    <i v-if="activeStep > index || (activeStep === 4 && isSuccessResult)" class="el-icon-check" />
                    <span v-else>{{ index + 1 }}</span>
                  </span>
                  <span
                    v-if="index < stepKeys.length - 1"
                    class="setup-roadmap-connector"
                    aria-hidden="true"
                  />
                </div>
                <span class="setup-roadmap-text">{{ $t(key) }}</span>
              </li>
            </ol>
          </div>
          <p class="setup-intro-note">{{ $t('upgrade.introduce2') }}</p>
        </aside>

        <main class="setup-main">
          <div class="setup-panel">
            <header class="setup-panel-header">
              <div class="setup-panel-header-row">
                <h2 class="setup-panel-title">{{ currentStepTitle }}</h2>
                <span class="setup-panel-step">{{ $t('setup.progress', { current: activeStep + 1, total: stepKeys.length }) }}</span>
              </div>
              <el-progress
                :percentage="progressPercent"
                :show-text="false"
                :stroke-width="4"
                class="setup-progress-bar"
              />
            </header>

            <div class="setup-panel-body">
              <div v-show="activeStep === 0" class="setup-step-panel">
                <el-descriptions :column="1" border size="small" class="setup-summary">
                  <el-descriptions-item :label="$t('upgrade.overview.currentVersion')">{{ preflight.currentVersion || '-' }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('upgrade.overview.targetVersion')">{{ status.targetVersion || '-' }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('upgrade.overview.databaseType')">{{ (preflight.databaseType || form.databaseType || '-').toUpperCase() }}</el-descriptions-item>
                  <el-descriptions-item :label="$t('upgrade.overview.pendingMigrations')">
                    <div v-if="migrationList.length">
                      <el-tag
                        v-for="item in migrationList"
                        :key="item"
                        size="mini"
                        type="warning"
                        class="upgrade-tag"
                      >
                        {{ item }}
                      </el-tag>
                    </div>
                    <span v-else>-</span>
                  </el-descriptions-item>
                </el-descriptions>
                <p class="setup-tip setup-tip--warn">{{ $t('upgrade.overview.backupReminder') }}</p>
                <p class="setup-tip">{{ $t('upgrade.overview.singleInstanceNotice') }}</p>
              </div>

              <div v-show="activeStep === 1" class="setup-step-panel">
                <p class="setup-step-desc">{{ $t('upgrade.diff.desc') }}</p>
                <el-table :data="diffRows" size="mini" border class="upgrade-diff-table" :row-class-name="diffRowClassName">
                  <el-table-column prop="key" :label="$t('upgrade.diff.key')" min-width="240" />
                  <el-table-column prop="currentValue" :label="$t('upgrade.diff.currentValue')" min-width="180">
                    <template slot-scope="scope">
                      <span>{{ formatDiffValue(scope.row.currentValue) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="suggestedValue" :label="$t('upgrade.diff.suggestedValue')" min-width="180">
                    <template slot-scope="scope">
                      <span>{{ formatDiffValue(scope.row.suggestedValue) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="action" :label="$t('upgrade.diff.action')" width="150">
                    <template slot-scope="scope">
                      <el-tag :type="isMissingRow(scope.row) ? 'warning' : 'info'" size="mini">
                        {{ isMissingRow(scope.row) ? $t('upgrade.diff.fillMissing') : $t('upgrade.diff.preserve') }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <p class="setup-tip">
                  {{ $t('upgrade.diff.missingSummary', { count: missingDiffCount }) }}
                </p>
              </div>

              <div v-show="activeStep === 2" class="setup-step-panel">
                <el-form ref="preflightForm" :model="form" label-position="top" size="small" class="setup-inner-form">
                  <el-form-item :label="$t('setup.database.type')" class="setup-form-item-compact">
                    <el-input :value="form.databaseType.toUpperCase()" disabled />
                  </el-form-item>
                  <div v-if="form.databaseType === 'mysql'" class="setup-form-row">
                    <el-form-item :label="$t('setup.database.host')" class="setup-form-row-item">
                      <el-input v-model="form.mysqlHost" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.port')" class="setup-form-row-item setup-form-row-item--sm">
                      <el-input-number v-model="form.mysqlPort" :min="1" :max="65535" controls-position="right" class="setup-full-width" />
                    </el-form-item>
                  </div>
                  <el-form-item v-if="form.databaseType === 'mysql'" :label="$t('setup.database.database')">
                    <el-input v-model="form.mysqlDatabase" />
                  </el-form-item>
                  <div v-if="form.databaseType === 'mysql'" class="setup-form-row">
                    <el-form-item :label="$t('setup.database.username')" class="setup-form-row-item">
                      <el-input v-model="form.mysqlUsername" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.database.password')" class="setup-form-row-item">
                      <el-input v-model="form.mysqlPassword" type="password" show-password />
                    </el-form-item>
                  </div>
                  <div v-if="form.databaseType === 'mysql'" class="setup-test-row">
                    <el-button :loading="dbTesting" @click="handleTestDatabase">{{ $t('setup.testConnection') }}</el-button>
                    <span v-if="dbTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                  </div>
                  <p v-else class="setup-tip setup-tip--success"><i class="el-icon-circle-check" /> {{ $t('upgrade.preflight.h2AutoPass') }}</p>

                  <el-form-item :label="$t('setup.cache.type')" class="setup-form-item-compact">
                    <el-input :value="cacheTypeLabel" disabled />
                  </el-form-item>
                  <div v-if="form.cacheType === 'redis'" class="setup-form-row">
                    <el-form-item :label="$t('setup.cache.host')" class="setup-form-row-item">
                      <el-input v-model="form.redisHost" />
                    </el-form-item>
                    <el-form-item :label="$t('setup.cache.port')" class="setup-form-row-item setup-form-row-item--sm">
                      <el-input-number v-model="form.redisPort" :min="1" :max="65535" controls-position="right" class="setup-full-width" />
                    </el-form-item>
                  </div>
                  <div v-if="form.cacheType === 'redis'" class="setup-form-row">
                    <el-form-item :label="$t('setup.cache.password')" class="setup-form-row-item">
                      <el-input v-model="form.redisPassword" type="password" show-password />
                    </el-form-item>
                    <el-form-item :label="$t('setup.cache.database')" class="setup-form-row-item setup-form-row-item--sm">
                      <el-input-number v-model="form.redisDatabase" :min="0" :max="15" controls-position="right" class="setup-full-width" />
                    </el-form-item>
                  </div>
                  <div v-if="form.cacheType === 'redis'" class="setup-test-row">
                    <el-button :loading="redisTesting" @click="handleTestRedis">{{ $t('setup.testConnection') }}</el-button>
                    <span v-if="redisTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                  </div>
                  <p v-else class="setup-tip setup-tip--success"><i class="el-icon-circle-check" /> {{ $t('upgrade.preflight.localCacheAutoPass') }}</p>

                  <el-form-item :label="$t('setup.storage.profile')">
                    <el-input v-model="form.profile" />
                  </el-form-item>
                  <el-form-item :label="$t('setup.storage.logPath')">
                    <el-input v-model="form.logPath" />
                  </el-form-item>
                  <div class="setup-test-row">
                    <el-button :loading="pathTesting" @click="handleTestPath">{{ $t('setup.testPath') }}</el-button>
                    <span v-if="pathTestPassed" class="setup-test-ok"><i class="el-icon-success" /> {{ $t('setup.testPassed') }}</span>
                  </div>
                </el-form>
              </div>

              <div v-show="activeStep === 3" class="setup-step-panel">
                <p class="setup-step-desc">{{ $t('upgrade.execute.desc') }}</p>
                <el-alert
                  :title="$t('upgrade.execute.currentState', { state: stateLabel })"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <el-steps :active="executeStepIndex" finish-status="success" align-center class="upgrade-steps">
                  <el-step :title="$t('upgrade.execute.stepConfig')" />
                  <el-step :title="$t('upgrade.execute.stepMigration')" />
                  <el-step :title="$t('upgrade.execute.stepRestart')" />
                </el-steps>
                <p v-if="status.lastError" class="setup-tip setup-tip--warn">{{ status.lastError }}</p>
              </div>

              <div v-show="activeStep === 4" class="setup-step-panel">
                <div v-if="isSuccessResult" class="setup-success setup-panel-fill">
                  <i class="el-icon-success setup-success-icon" />
                  <h3>{{ $t('upgrade.success.title') }}</h3>
                  <p>{{ $t('upgrade.success.message') }}</p>
                  <p v-if="waitingForRestart" class="setup-tip setup-tip--warn">{{ $t('upgrade.success.restarting') }}</p>
                  <p v-else class="setup-tip setup-tip--warn">{{ $t('upgrade.success.restartPrompt') }}</p>
                </div>
                <div v-else class="setup-success setup-panel-fill">
                  <i class="el-icon-error" style="font-size:64px;color:#f56c6c;" />
                  <h3>{{ $t('upgrade.failure.title') }}</h3>
                  <p>{{ status.lastError || $t('upgrade.failure.message') }}</p>
                  <p class="setup-tip">{{ $t('upgrade.failure.lastStep', { step: status.lastStep || '-' }) }}</p>
                </div>
              </div>
            </div>

            <footer class="setup-panel-footer">
              <div class="setup-panel-footer-lang">
                <label class="setup-lang-label" for="upgrade-lang-select">{{ $t('setup.language') }}</label>
                <div class="setup-lang-control">
                  <svg-icon :icon-class="currentLangIcon" class="setup-lang-flag" />
                  <el-select
                    id="upgrade-lang-select"
                    :value="currentLocale"
                    size="small"
                    class="setup-lang-select"
                    @change="changeLang"
                  >
                    <el-option
                      v-for="lang in langOptions"
                      :key="lang.code"
                      :label="lang.label"
                      :value="lang.code"
                    >
                      <span class="setup-lang-option">
                        <svg-icon :icon-class="lang.icon" />
                        <span>{{ lang.label }}</span>
                      </span>
                    </el-option>
                  </el-select>
                </div>
              </div>
              <div class="setup-panel-footer-nav">
                <el-button v-if="activeStep > 0 && activeStep < 4" size="small" @click="prevStep">{{ $t('setup.prev') }}</el-button>
                <el-button v-if="activeStep < 3" type="primary" size="small" :loading="preflightChecking" @click="nextStep">{{ $t('setup.next') }}</el-button>
                <el-button v-if="activeStep === 3 && !status.restartRequired && status.state !== 'restart_required'" type="primary" size="small" :loading="executing" @click="handleExecute">
                  {{ isFailureResult ? $t('upgrade.execute.retry') : $t('upgrade.execute.submit') }}
                </el-button>
                <el-button v-if="activeStep === 4 && isFailureResult" type="danger" size="small" :loading="executing" @click="retryNow">
                  {{ $t('upgrade.execute.retry') }}
                </el-button>
                <el-button v-if="activeStep === 4 && isSuccessResult" type="primary" size="small" @click="goToLogin">
                  {{ $t('upgrade.success.goLogin') }}
                </el-button>
              </div>
            </footer>
          </div>
        </main>
      </div>

      <p class="setup-copyright">{{ $t('copyright') }}</p>
    </div>
  </div>
</template>

<script>
import { getUpgradePreflight, getUpgradeStatus, retryUpgrade, submitUpgrade } from '@/api/upgrade'
import { getSetupStatus, testDatabase, testPath, testRedis } from '@/api/setup'
import { resetUpgradeStatusCache } from '@/utils/upgrade-status'
import { resetSetupStatusCache } from '@/utils/setup-status'
import store from '@/store'

const I18N_LOCALE_KEY = 'i18n-locale'
const UPGRADE_AWAITING_READY_KEY = 'upgrade-awaiting-ready'

const STEP_KEYS = [
  'upgrade.step.overview',
  'upgrade.step.diff',
  'upgrade.step.preflight',
  'upgrade.step.execute',
  'upgrade.step.result'
]

function isTestSuccess(res) {
  if (res == null) return false
  if (res.success === true) return true
  if (res.success === false) return false
  return res.code === 200 || res.code === undefined
}

function testErrorMessage(res, fallback) {
  return (res && (res.message || res.msg)) || fallback
}

export default {
  name: 'UpgradeWizard',
  data() {
    return {
      stepKeys: STEP_KEYS,
      langOptions: [
        { code: 'zh_CN', label: '简体中文', icon: 'lang_zh_CN' },
        { code: 'zh_TW', label: '繁體中文', icon: 'lang_zh_CN' },
        { code: 'en_US', label: 'English', icon: 'lang_en_US' },
        { code: 'ru', label: 'Русский', icon: 'lang_ru' },
        { code: 'ja_JP', label: '日本語', icon: 'lang_ja_JP' },
        { code: 'ko_KR', label: '한국어', icon: 'lang_ko_KR' },
        { code: 'ar', label: 'العربية', icon: 'lang_ar' }
      ],
      activeStep: 0,
      executing: false,
      dbTesting: false,
      redisTesting: false,
      pathTesting: false,
      preflightChecking: false,
      dbTestPassed: false,
      redisTestPassed: false,
      pathTestPassed: false,
      waitingForRestart: false,
      waitElapsed: 0,
      waitElapsedTimer: null,
      waitPollTimer: null,
      status: {
        upgradeRequired: false,
        state: '',
        targetVersion: '',
        pendingMigrations: [],
        lastError: '',
        lastStep: '',
        restartRequired: false
      },
      preflight: {
        currentVersion: '',
        databaseType: '',
        diffs: []
      },
      form: {
        databaseType: 'h2',
        mysqlHost: '127.0.0.1',
        mysqlPort: 3306,
        mysqlDatabase: 'cat2bug_platform',
        mysqlUsername: 'root',
        mysqlPassword: '',
        cacheType: 'local',
        redisHost: '127.0.0.1',
        redisPort: 6379,
        redisPassword: '',
        redisDatabase: 0,
        profile: './upload',
        temp: './uploadTemp',
        logPath: './logs',
        aiEnabled: false,
        aiHost: 'http://127.0.0.1:11434'
      }
    }
  },
  computed: {
    currentLocale() {
      return this.$i18n.locale
    },
    currentLangIcon() {
      const found = this.langOptions.find(item => item.code === this.currentLocale)
      return found ? found.icon : 'lang_zh_CN'
    },
    isRtl() {
      return this.currentLocale === 'ar'
    },
    progressPercent() {
      return Math.round(((this.activeStep + 1) / this.stepKeys.length) * 100)
    },
    currentStepTitle() {
      return this.$t(this.stepKeys[this.activeStep] || 'upgrade.title')
    },
    migrationList() {
      const list = Array.isArray(this.status.pendingMigrations) ? this.status.pendingMigrations : []
      return list
    },
    diffRows() {
      const list = Array.isArray(this.preflight.diffs) ? this.preflight.diffs : []
      return list.map((item, index) => ({
        id: item.key || item.name || `diff-${index}`,
        key: item.key || item.name || '-',
        currentValue: item.currentValue,
        suggestedValue: item.suggestedValue,
        missing: item.missing === true || item.action === 'missing' || item.status === 'missing'
      }))
    },
    missingDiffCount() {
      return this.diffRows.filter(row => this.isMissingRow(row)).length
    },
    cacheTypeLabel() {
      return this.form.cacheType === 'redis'
        ? this.$t('setup.cache.redis')
        : this.$t('setup.cache.local')
    },
    executeStepIndex() {
      if (this.status.restartRequired || this.status.state === 'restart_required') return 3
      if (this.status.state === 'completed' && !this.status.upgradeRequired) return 3
      if (this.status.lastStep === 'restart') return 3
      if (this.status.lastStep === 'migration') return 2
      if (this.status.lastStep === 'config') return 1
      if (this.status.state === 'running') return 1
      return 0
    },
    stateLabel() {
      const key = this.status.state || 'pending'
      return this.$t(`upgrade.state.${key}`)
    },
    isSuccessResult() {
      if (this.status.restartRequired || this.status.state === 'restart_required') {
        return true
      }
      return this.status.state === 'completed' && !this.status.upgradeRequired
    },
    isFailureResult() {
      return this.status.state === 'failed'
    },
    brandLogoSrc() {
      return require('@/assets/images/update-wizard-mascot.png')
    }
  },
  created() {
    document.documentElement.classList.add('setup-wizard-active')
    const lang = this.$cache.local.get(I18N_LOCALE_KEY) || 'zh_CN'
    this.$i18n.locale = lang
    this.initialize()
  },
  beforeDestroy() {
    document.documentElement.classList.remove('setup-wizard-active')
    this.clearWaitTimers()
  },
  methods: {
    changeLang(lang) {
      this.$i18n.locale = lang
      this.$cache.local.set(I18N_LOCALE_KEY, lang)
    },
    initialize() {
      Promise.all([this.refreshStatus(), this.loadPreflight()]).then(() => {
        if (this.isSuccessResult || this.isFailureResult) {
          this.activeStep = 4
        }
        if (this.isSuccessResult || sessionStorage.getItem(UPGRADE_AWAITING_READY_KEY) === '1') {
          this.waitingForRestart = true
          this.startWaitingForReady()
        }
      }).catch(() => {
        if (sessionStorage.getItem(UPGRADE_AWAITING_READY_KEY) === '1') {
          this.activeStep = 4
          this.status.restartRequired = true
          this.status.state = 'restart_required'
          this.waitingForRestart = true
          this.startWaitingForReady()
        }
      })
    },
    refreshStatus() {
      return getUpgradeStatus().then(res => {
        const payload = (res && res.data) ? res.data : (res || {})
        this.status = {
          upgradeRequired: payload.upgradeRequired === true,
          state: payload.state || '',
          targetVersion: payload.targetVersion || '',
          pendingMigrations: Array.isArray(payload.pendingMigrations) ? payload.pendingMigrations : [],
          lastError: payload.lastError || '',
          lastStep: payload.lastStep || '',
          restartRequired: payload.restartRequired === true
        }
      })
    },
    loadPreflight() {
      return getUpgradePreflight().then(res => {
        const payload = (res && res.data) ? res.data : (res || {})
        this.preflight = payload || {}
        this.patchFormFromPreflight(payload || {})
      }).catch(() => {})
    },
    patchFormFromPreflight(preflight) {
      if (!preflight) return
      this.form.databaseType = preflight.databaseType || this.form.databaseType
      this.form.cacheType = preflight.cacheType || this.form.cacheType
      this.form.profile = preflight.profile || this.form.profile
      this.form.temp = preflight.temp || this.form.temp
      this.form.logPath = preflight.logPath || this.form.logPath
      this.form.aiEnabled = preflight.aiEnabled === true
      this.form.aiHost = preflight.aiHost || this.form.aiHost

      this.form.mysqlHost = preflight.mysqlHost || this.form.mysqlHost
      this.form.mysqlPort = preflight.mysqlPort || this.form.mysqlPort
      this.form.mysqlDatabase = preflight.mysqlDatabase || this.form.mysqlDatabase
      this.form.mysqlUsername = preflight.mysqlUsername || this.form.mysqlUsername
      this.form.mysqlPassword = preflight.mysqlPassword || this.form.mysqlPassword

      this.form.redisHost = preflight.redisHost || this.form.redisHost
      this.form.redisPort = preflight.redisPort || this.form.redisPort
      this.form.redisPassword = preflight.redisPassword || this.form.redisPassword
      this.form.redisDatabase = preflight.redisDatabase !== undefined ? preflight.redisDatabase : this.form.redisDatabase
    },
    isMissingRow(row) {
      return row && row.missing === true
    },
    diffRowClassName({ row }) {
      return this.isMissingRow(row) ? 'upgrade-diff-row--missing' : ''
    },
    formatDiffValue(value) {
      if (value === undefined || value === null || value === '') return '-'
      if (typeof value === 'object') return JSON.stringify(value)
      return String(value)
    },
    prevStep() {
      if (this.activeStep > 0) {
        this.activeStep -= 1
      }
    },
    nextStep() {
      if (this.activeStep === 2) {
        this.validatePreflight().then(() => {
          this.activeStep = 3
        }).catch(err => {
          if (err && err.message) {
            this.$message.error(err.message)
          }
        })
        return
      }
      if (this.activeStep < 3) {
        this.activeStep += 1
      }
    },
    buildDatabasePayload() {
      const payload = { databaseType: this.form.databaseType }
      if (this.form.databaseType === 'mysql') {
        payload.host = this.form.mysqlHost
        payload.port = this.form.mysqlPort
        payload.database = this.form.mysqlDatabase
        payload.username = this.form.mysqlUsername
        payload.password = this.form.mysqlPassword
      }
      return payload
    },
    buildRedisPayload() {
      return {
        host: this.form.redisHost,
        port: this.form.redisPort,
        password: this.form.redisPassword || undefined,
        database: this.form.redisDatabase
      }
    },
    buildSubmitPayload() {
      const payload = {
        databaseType: this.form.databaseType,
        cacheType: this.form.cacheType,
        profile: this.form.profile,
        temp: this.form.temp,
        logPath: this.form.logPath,
        aiEnabled: this.form.aiEnabled,
        aiHost: this.form.aiHost
      }
      if (this.form.databaseType === 'mysql') {
        payload.mysqlHost = this.form.mysqlHost
        payload.mysqlPort = this.form.mysqlPort
        payload.mysqlDatabase = this.form.mysqlDatabase
        payload.mysqlUsername = this.form.mysqlUsername
        payload.mysqlPassword = this.form.mysqlPassword
      }
      if (this.form.cacheType === 'redis') {
        payload.redisHost = this.form.redisHost
        payload.redisPort = this.form.redisPort
        payload.redisPassword = this.form.redisPassword || undefined
        payload.redisDatabase = this.form.redisDatabase
      }
      return payload
    },
    runDatabaseTest(silent = false) {
      if (this.form.databaseType !== 'mysql') {
        this.dbTestPassed = true
        return Promise.resolve(true)
      }
      if (this.dbTestPassed) {
        return Promise.resolve(true)
      }
      this.dbTesting = true
      return testDatabase(this.buildDatabasePayload())
        .then(res => {
          if (isTestSuccess(res)) {
            this.dbTestPassed = true
            if (!silent) {
              this.$message.success(this.$t('setup.testPassed'))
            }
            return true
          }
          const msg = testErrorMessage(res, this.$t('setup.testFailed'))
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(new Error(msg))
        })
        .catch(err => {
          const msg = err && err.message ? err.message : this.$t('setup.testFailed')
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(err instanceof Error ? err : new Error(msg))
        })
        .finally(() => {
          this.dbTesting = false
        })
    },
    runRedisTest(silent = false) {
      if (this.form.cacheType !== 'redis') {
        this.redisTestPassed = true
        return Promise.resolve(true)
      }
      if (this.redisTestPassed) {
        return Promise.resolve(true)
      }
      this.redisTesting = true
      return testRedis(this.buildRedisPayload())
        .then(res => {
          if (isTestSuccess(res)) {
            this.redisTestPassed = true
            if (!silent) {
              this.$message.success(this.$t('setup.testPassed'))
            }
            return true
          }
          const msg = testErrorMessage(res, this.$t('setup.testFailed'))
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(new Error(msg))
        })
        .catch(err => {
          const msg = err && err.message ? err.message : this.$t('setup.testFailed')
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(err instanceof Error ? err : new Error(msg))
        })
        .finally(() => {
          this.redisTesting = false
        })
    },
    runPathTest(silent = false) {
      const profile = (this.form.profile || '').trim()
      const logPath = (this.form.logPath || '').trim()
      if (!profile || !logPath) {
        const msg = this.$t('upgrade.preflight.pathRequired').toString()
        if (!silent) {
          this.$message.warning(msg)
        }
        return Promise.reject(new Error(msg))
      }
      if (this.pathTestPassed) {
        return Promise.resolve(true)
      }
      this.pathTesting = true
      return testPath({ path: profile })
        .then(res => {
          if (!isTestSuccess(res)) {
            throw new Error(testErrorMessage(res, this.$t('setup.testFailed')))
          }
          return testPath({ path: logPath })
        })
        .then(res => {
          if (isTestSuccess(res)) {
            this.pathTestPassed = true
            if (!silent) {
              this.$message.success(this.$t('setup.testPassed'))
            }
            return true
          }
          const msg = testErrorMessage(res, this.$t('setup.testFailed'))
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(new Error(msg))
        })
        .catch(err => {
          const msg = err && err.message ? err.message : this.$t('setup.testFailed')
          if (!silent) {
            this.$message.error(msg)
          }
          return Promise.reject(err instanceof Error ? err : new Error(msg))
        })
        .finally(() => {
          this.pathTesting = false
        })
    },
    handleTestDatabase() {
      this.runDatabaseTest(false).catch(() => {})
    },
    handleTestRedis() {
      this.runRedisTest(false).catch(() => {})
    },
    handleTestPath() {
      this.runPathTest(false).catch(() => {})
    },
    validatePreflight() {
      this.preflightChecking = true
      return this.runDatabaseTest(true)
        .then(() => this.runRedisTest(true))
        .then(() => this.runPathTest(true))
        .finally(() => {
          this.preflightChecking = false
        })
    },
    handleExecute() {
      if (this.status.restartRequired || this.status.state === 'restart_required') {
        this.activeStep = 4
        this.waitingForRestart = true
        this.startWaitingForReady()
        return
      }
      this.executing = true
      const payload = this.buildSubmitPayload()
      const requestFn = this.isFailureResult ? retryUpgrade : submitUpgrade
      requestFn(payload)
        .then(res => {
          resetUpgradeStatusCache()
          const data = (res && res.data) ? res.data : (res || {})
          this.status.restartRequired = data.restartRequired === true
          if (this.status.restartRequired) {
            this.status.state = 'restart_required'
          }
          this.activeStep = 4
          if (this.isSuccessResult) {
            sessionStorage.setItem(UPGRADE_AWAITING_READY_KEY, '1')
            this.waitingForRestart = true
            this.startWaitingForReady()
            this.$message.success(this.$t('upgrade.success.message'))
          } else if (this.isFailureResult) {
            this.$message.error(this.status.lastError || this.$t('upgrade.failure.message'))
          }
        })
        .catch(() => {})
        .finally(() => {
          this.executing = false
        })
    },
    retryNow() {
      this.activeStep = 3
      this.handleExecute()
    },
    goToLogin() {
      this.clearWaitTimers()
      sessionStorage.removeItem(UPGRADE_AWAITING_READY_KEY)
      resetUpgradeStatusCache()
      resetSetupStatusCache()
      store.dispatch('FedLogOut').finally(() => {
        window.location.href = '/login'
      })
    },
    startWaitingForReady() {
      this.clearWaitTimers()
      this.waitElapsed = 0
      this.waitElapsedTimer = setInterval(() => {
        this.waitElapsed += 1
      }, 1000)
      this.pollApplicationReady()
      this.waitPollTimer = setInterval(() => {
        this.pollApplicationReady()
      }, 2000)
    },
    pollApplicationReady() {
      resetSetupStatusCache()
      getSetupStatus()
        .then(res => {
          const payload = (res && res.data) ? res.data : (res || {})
          if (payload.installed === true && payload.restartRequired !== true) {
            this.goToLogin()
          }
        })
        .catch(() => {})
    },
    clearWaitTimers() {
      if (this.waitElapsedTimer) {
        clearInterval(this.waitElapsedTimer)
        this.waitElapsedTimer = null
      }
      if (this.waitPollTimer) {
        clearInterval(this.waitPollTimer)
        this.waitPollTimer = null
      }
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
$setup-text: #303133;
$setup-muted: #606266;
$setup-muted-light: #909399;
$setup-border: #e4e7ed;
$setup-aside-bg: #f5f7fa;
$setup-wm-tile: 168px;
$setup-wm-pattern: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='168' height='168' viewBox='0 0 168 168'%3E%3Ctext x='8' y='96' font-family='system-ui,-apple-system,sans-serif' font-size='24' font-weight='700' fill='rgba(64,158,255,0.1)' letter-spacing='0.06em'%3ECat2Bug%3C/text%3E%3C/svg%3E");

.setup-page {
  position: relative;
  width: 100%;
  max-width: 100vw;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: linear-gradient(165deg, #f0f5fc 0%, #f8fafc 42%, #fff 100%);
  box-sizing: border-box;
}

.setup-page-watermark {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.setup-page-watermark__pattern {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 320vmax;
  height: 320vmax;
  margin-top: -160vmax;
  margin-left: -160vmax;
  background-image: $setup-wm-pattern;
  background-repeat: repeat;
  background-size: $setup-wm-tile $setup-wm-tile;
  transform: rotate(-45deg);
  animation: setup-watermark-bg 32s linear infinite;
}

@keyframes setup-watermark-bg {
  0% { background-position: 0 0; }
  100% { background-position: $setup-wm-tile 0; }
}

.setup-layout {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 20px 40px 10px;
  box-sizing: border-box;
}

.setup-hero {
  position: relative;
  flex-shrink: 0;
  margin: 0 0 12px;
  padding-inline-end: 320px;
  border-radius: 12px;
  overflow: visible;
}

.setup-hero-bg {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(105deg, rgba(255, 255, 255, 0.92) 0%, rgba(245, 247, 250, 0.88) 55%, rgba(236, 244, 255, 0.5) 100%);
  border: 1px solid rgba(228, 231, 237, 0.9);
}

.setup-hero-head {
  position: relative;
  z-index: 2;
  max-width: 640px;
  padding: 40px 0 40px 20px;

  h1 {
    margin: 0 0 6px;
    font-size: 20px;
    font-weight: 600;
    color: $setup-text;
    line-height: 1.3;
  }
}

.setup-hero-art {
  position: absolute;
  top: -15px;
  left: max(0px, calc(100% - 448px));
  width: 440px;
  pointer-events: none;
}

.setup-hero-art-img {
  width: 440px;
  height: 160px;
  object-fit: contain;
  object-position: right bottom;
}

.setup-intro-lead {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: $setup-muted;
}

.setup-body {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  gap: 20px;
}

.setup-aside {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 10px;
  background: $setup-aside-bg;
  border: 1px solid $setup-border;
  border-radius: 12px;
}

.setup-aside-content {
  flex: 1;
  overflow-y: auto;
  padding-inline: 12px;
}

.setup-roadmap-title {
  margin: 0 0 8px;
  padding-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: $setup-text;
  border-bottom: 1px solid #ebeef5;
}

.setup-intro-note {
  margin: auto 12px 0;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.5;
  color: $setup-muted;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.setup-roadmap {
  list-style: none;
  margin: 0;
  padding: 0;
}

.setup-roadmap-item {
  display: flex;
  gap: 12px;
  min-height: 44px;

  &.is-active .setup-roadmap-marker {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }

  &.is-done .setup-roadmap-marker {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }
}

.setup-roadmap-marker-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 28px;
}

.setup-roadmap-marker {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: $setup-muted-light;
  background: #fff;
}

.setup-roadmap-connector {
  flex: 1;
  width: 2px;
  margin: 4px 0;
  background: #e4e7ed;
}

.setup-roadmap-text {
  padding-top: 4px;
  font-size: 14px;
  color: $setup-muted;
}

.setup-main {
  display: flex;
  min-height: 0;
}

.setup-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid $setup-border;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.setup-panel-header {
  flex-shrink: 0;
  padding: 12px 18px 10px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}

.setup-panel-header-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.setup-panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: $setup-text;
}

.setup-panel-step {
  font-size: 12px;
  color: $setup-muted-light;
}

.setup-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 18px;
}

.setup-panel-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}

.setup-panel-footer-lang,
.setup-panel-footer-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setup-step-panel {
  animation: setup-in 0.2s ease;
}

@keyframes setup-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.setup-inner-form ::v-deep .el-form-item {
  margin-bottom: 12px;
}

.setup-form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.setup-form-row-item {
  flex: 1 1 180px;

  &--sm {
    flex: 0 1 110px;
  }
}

.setup-full-width {
  width: 100%;
}

.setup-tip {
  margin: 8px 0 0;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: $setup-muted;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;

  &--warn {
    background: #fdf6ec;
    border-color: #faecd8;
    color: #b88230;
  }

  &--success {
    background: #f0f9eb;
    border-color: #e1f3d8;
    color: #529b2e;
  }
}

.setup-step-desc {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.6;
  color: $setup-muted;
}

.setup-summary {
  margin-bottom: 12px;
}

.setup-test-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.setup-test-ok {
  font-size: 13px;
  color: #67c23a;
}

.setup-lang-label {
  font-size: 12px;
  color: $setup-muted;
}

.setup-lang-control {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.setup-lang-flag {
  font-size: 22px;
}

.setup-lang-select {
  width: 148px;
}

.setup-lang-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.setup-success {
  text-align: center;
  padding: 24px 20px;

  h3 {
    margin: 16px 0 8px;
    color: $setup-text;
  }
}

.setup-success-icon {
  font-size: 64px;
  color: #67c23a;
}

.setup-copyright {
  flex-shrink: 0;
  margin: 10px 0 0;
  font-size: 12px;
  color: $setup-muted-light;
  text-align: center;
}

.upgrade-tag {
  margin: 2px 6px 2px 0;
}

.upgrade-steps {
  margin-top: 16px;
}

.upgrade-diff-table ::v-deep .upgrade-diff-row--missing {
  background: #fff8e1;
}

@media screen and (max-width: 980px) {
  .setup-page {
    height: auto;
    max-height: none;
    overflow: auto;
  }

  .setup-layout {
    height: auto;
    padding: 12px 16px 20px;
  }

  .setup-body {
    grid-template-columns: 1fr;
  }

  .setup-aside {
    display: none;
  }

  .setup-panel-footer {
    flex-direction: column;
    align-items: stretch;
  }
}

@media screen and (max-width: 550px) {
  .setup-hero {
    padding-inline-end: 0;
  }

  .setup-hero-art {
    display: none;
  }
}
</style>

<style lang="scss">
html.setup-wizard-active,
html.setup-wizard-active body {
  width: 100%;
  max-width: 100vw;
  height: 100%;
  margin: 0;
  overflow: hidden;
}

html.setup-wizard-active #app {
  width: 100%;
  max-width: 100vw;
  height: 100%;
  overflow-x: hidden;
}

@media screen and (max-width: 980px) {
  html.setup-wizard-active,
  html.setup-wizard-active body {
    overflow-x: hidden;
    overflow-y: auto;
  }
}
</style>
