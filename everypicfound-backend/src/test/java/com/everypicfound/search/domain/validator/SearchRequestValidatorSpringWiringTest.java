package com.everypicfound.search.domain.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;

@SpringJUnitConfig(SearchRequestValidatorSpringWiringTest.TestConfig.class)
@TestPropertySource(properties = {
        "everypicfound.search.max-top-k=33",
        "everypicfound.search.max-query-text-length=44"
})
class SearchRequestValidatorSpringWiringTest {

    @Autowired
    private List<SearchRequestValidator> validators;

    @Autowired
    private SearchProperties searchProperties;

    @Test
    void validatorsAreRegisteredAsSpringBeansBySearchType() {
        Map<SearchType, SearchRequestValidator> validatorMap = validators.stream()
                .collect(Collectors.toMap(SearchRequestValidator::supportType, Function.identity()));

        assertThat(validatorMap).containsOnlyKeys(SearchType.IMAGE, SearchType.TEXT, SearchType.HYBRID);
        assertThat(validatorMap.get(SearchType.IMAGE)).isInstanceOf(ImageSearchRequestValidator.class);
        assertThat(validatorMap.get(SearchType.TEXT)).isInstanceOf(TextSearchRequestValidator.class);
        assertThat(validatorMap.get(SearchType.HYBRID)).isInstanceOf(HybridSearchRequestValidator.class);
    }

    @Test
    void searchPropertiesAreBoundForValidatorUse() {
        assertThat(searchProperties.getMaxTopK()).isEqualTo(33);
        assertThat(searchProperties.getMaxQueryTextLength()).isEqualTo(44);
    }

    @Configuration
    @EnableConfigurationProperties(SearchProperties.class)
    @Import({
            ImageSearchRequestValidator.class,
            TextSearchRequestValidator.class,
            HybridSearchRequestValidator.class
    })
    static class TestConfig {
    }
}
