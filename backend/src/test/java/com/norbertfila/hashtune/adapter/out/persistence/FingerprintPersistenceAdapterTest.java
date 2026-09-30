package com.norbertfila.hashtune.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class FingerprintPersistenceAdapterTest {
    private final SpringDataFingerprintRepository repository = mock(SpringDataFingerprintRepository.class);
    private final FingerprintPersistenceAdapter adapter = new FingerprintPersistenceAdapter(repository);

    @Test
    void batchesLargeHashLookups() {
        List<Long> hashes = LongStream.range(0, 20_001).boxed().toList();
        when(repository.findByHashIn(anyCollection())).thenReturn(List.of());

        assertThat(adapter.findMatches(hashes)).isEmpty();

        verify(repository, org.mockito.Mockito.times(3)).findByHashIn(anyCollection());
    }
}
