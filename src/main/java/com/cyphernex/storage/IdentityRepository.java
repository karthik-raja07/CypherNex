package com.cyphernex.storage;

import com.cyphernex.model.IdentityRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IdentityRepository {

    private final Map<String, IdentityRecord> inMemoryStore = new ConcurrentHashMap<>();

    public void save(IdentityRecord record) {
        if (record != null && record.getCanonicalIdentity() != null) {
            inMemoryStore.put(record.getCanonicalIdentity(), record);
        }
    }

    public Optional<IdentityRecord> findByCanonicalIdentity(String canonicalIdentity) {
        if (canonicalIdentity == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inMemoryStore.get(canonicalIdentity));
    }

    public List<IdentityRecord> findAll() {
        return new ArrayList<>(inMemoryStore.values());
    }
}
