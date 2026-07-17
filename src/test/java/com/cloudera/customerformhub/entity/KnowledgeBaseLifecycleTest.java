package com.cloudera.customerformhub.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeBaseLifecycleTest {

    @Test
    void derivesApprovedStatusForLegacyRows() {
        KnowledgeBase entry = new KnowledgeBase();
        entry.setApproved(true);

        entry.beforeInsert();

        assertEquals("Approved", entry.getStatus());
        assertTrue(entry.getApproved());
    }

    @Test
    void keepsApprovedFlagAlignedWithLifecycleStatus() {
        KnowledgeBase entry = new KnowledgeBase();
        entry.setApproved(true);
        entry.setStatus("Archived");

        entry.beforeUpdate();

        assertEquals("Archived", entry.getStatus());
        assertFalse(entry.getApproved());
    }
}
