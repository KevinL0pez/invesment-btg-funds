package com.btg.funds.infrastructure.adapters.dynamodb;

import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.infrastructure.adapters.dynamodb.entities.FundEntity;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Repository
@Slf4j
@Data
@Builder
@RequiredArgsConstructor
@DynamoDbBean
public class DynamoDbFundAdapter implements FundRepository {

    private final DynamoDbAsyncTable<FundEntity> fundTable;

    @Override
    public Mono<Fund> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Mono.fromFuture(fundTable.getItem(key))
                .flatMap(entity -> {
                    if (entity == null) {
                        return Mono.error(new FundNotFoundException(id));
                    }
                    return Mono.just(mapToDomain(entity));
                });
    }

    @Override
    public Flux<Fund> findAll() {
        return Flux.from(fundTable.scan().items())
                .map(this::toDomain)
                .doOnError(e -> log.error("Error al escanear la tabla de fondos: " + e.getMessage()));
    }

    @Override
    public Mono<Fund> save(Fund fund) {
        return Mono.fromFuture(fundTable.putItem(toEntity(fund)))
                .thenReturn(fund);
    }

    private Fund mapToDomain(FundEntity entity) {
        return Fund.builder()
                .id(entity.getId())
                .name(entity.getName())
                .minimumAmount(entity.getMinAmount())
                .category(entity.getCategory())
                .build();
    }

    private Fund toDomain(FundEntity entity) {
        return Fund.builder()
                .id(entity.getId())
                .name(entity.getName())
                .minimumAmount(entity.getMinAmount())
                .category(entity.getCategory())
                .build();
    }

    private FundEntity toEntity(Fund fund) {
        return FundEntity.builder()
                .id(fund.getId())
                .name(fund.getName())
                .minAmount(fund.getMinimumAmount())
                .category(fund.getCategory())
                .build();
    }
}
