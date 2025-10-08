package com.tut2.group3.bank.config;

import com.tut2.group3.bank.dto.DebitRequestDTO;
import com.tut2.group3.bank.dto.RefundRequestDTO;
import com.tut2.group3.bank.entity.Transaction;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1. Uses STRICT matching to avoid fuzzy auto-binding issues.
 * 2. Explicitly skips auto-generated ID fields to prevent accidental overrides.
 * 3. Keeps mappings centralized and consistent for long-term maintainability.
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Use STRICT mode (only match exact property names)
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // Define mapping for DebitRequestDTO → Transaction
        TypeMap<DebitRequestDTO, Transaction> debitMap =
                modelMapper.createTypeMap(DebitRequestDTO.class, Transaction.class);

        debitMap.addMappings(mapper -> {
            // Explicitly skip the database-generated primary key
            mapper.skip(Transaction::setId);
        });

        // Define mapping for RefundRequestDTO → Transaction
        TypeMap<RefundRequestDTO, Transaction> refundMap =
                modelMapper.createTypeMap(RefundRequestDTO.class, Transaction.class);

        refundMap.addMappings(mapper -> {
            // Skip auto-generated primary key as well
            mapper.skip(Transaction::setId);
        });

        return modelMapper;
    }
}