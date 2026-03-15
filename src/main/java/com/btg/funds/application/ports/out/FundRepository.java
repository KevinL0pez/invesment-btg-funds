package com.btg.funds.application.ports.out;

import com.btg.funds.domain.model.Fund;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FundRepository {

    Mono<Fund> findById(String id);

    Flux<Fund> findAll();

    Mono<Fund> save(Fund fund);
}