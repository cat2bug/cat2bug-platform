package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.system.mapper.SysDocumentMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件夹级联删除收集逻辑：非空夹须带上全部子孙 id。
 */
class SysDocumentCascadeDeleteTest {

    @Test
    void folderCollectsNestedChildren() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.put(folder(1L, 0L));
        mapper.put(folder(2L, 1L));
        mapper.put(file(3L, 2L));
        mapper.put(file(4L, 1L));

        SysDocumentService service = new SysDocumentService();
        inject(service, "sysDocumentMapper", mapper);

        Set<Long> out = new LinkedHashSet<>();
        service.collectCascadeDeleteIds(1L, out);

        assertEquals(Set.of(1L, 2L, 3L, 4L), out);
    }

    @Test
    void fileOnlyCollectsSelf() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.put(file(9L, 1L));

        SysDocumentService service = new SysDocumentService();
        inject(service, "sysDocumentMapper", mapper);

        Set<Long> out = new LinkedHashSet<>();
        service.collectCascadeDeleteIds(9L, out);

        assertEquals(Set.of(9L), out);
    }

    @Test
    void deleteIdsExpandsFoldersOnce() throws Exception {
        FakeMapper mapper = new FakeMapper();
        mapper.put(folder(1L, 0L));
        mapper.put(file(2L, 1L));
        mapper.put(file(3L, 0L));

        SysDocumentService service = new SysDocumentService();
        inject(service, "sysDocumentMapper", mapper);

        int rows = service.deleteSysDocumentByDocIds(new Long[]{1L, 3L});
        assertEquals(3, rows);
        assertTrue(mapper.deleted.containsAll(List.of(1L, 2L, 3L)));
        assertEquals(3, mapper.deleted.size());
    }

    private static SysDocument folder(Long id, Long pid) {
        SysDocument d = new SysDocument();
        d.setDocId(id);
        d.setDocPid(pid);
        d.setDocType(0L);
        d.setProjectId(100L);
        d.setDocName("f" + id);
        return d;
    }

    private static SysDocument file(Long id, Long pid) {
        SysDocument d = new SysDocument();
        d.setDocId(id);
        d.setDocPid(pid);
        d.setDocType(1L);
        d.setProjectId(100L);
        d.setDocName("n" + id);
        return d;
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    static final class FakeMapper implements SysDocumentMapper {
        final Map<Long, SysDocument> byId = new ConcurrentHashMap<>();
        final List<Long> deleted = new ArrayList<>();

        void put(SysDocument d) {
            byId.put(d.getDocId(), d);
        }

        @Override
        public long countSysDocumentList(SysDocument query) {
            return selectSysDocumentList(query, 0, Integer.MAX_VALUE).size();
        }

        @Override
        public List<SysDocument> selectSysDocumentList(SysDocument query, int offset, int limit) {
            return byId.values().stream()
                    .filter(d -> query.getProjectId() == null || query.getProjectId().equals(d.getProjectId()))
                    .filter(d -> {
                        if (query.getDocPid() == null) {
                            return true;
                        }
                        if (query.getDocPid() > 0) {
                            return query.getDocPid().equals(d.getDocPid());
                        }
                        return d.getDocPid() == null || d.getDocPid() <= 0;
                    })
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public SysDocument selectSysDocumentByDocId(Long docId) {
            return byId.get(docId);
        }

        @Override
        public int insertSysDocument(SysDocument sysDocument) {
            return 0;
        }

        @Override
        public int updateSysDocument(SysDocument sysDocument) {
            return 0;
        }

        @Override
        public int deleteSysDocumentByDocId(Long docId) {
            return byId.remove(docId) != null ? 1 : 0;
        }

        @Override
        public int deleteSysDocumentByDocIds(Long[] docIds) {
            int n = 0;
            for (Long id : docIds) {
                deleted.add(id);
                if (byId.remove(id) != null) {
                    n++;
                }
            }
            return n;
        }
    }
}
