package com.everypicfound.search.application.pipeline;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchContext;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.domain.validator.SearchRequestValidator;
import com.everypicfound.search.domain.validator.SearchValidateResult;
import com.everypicfound.search.error.SearchErrorCode;
/*
目前该部分代码已经通过 .\mvnw.cmd -q -DskipTests compile 编译验证，相关 Service 和 Pipeline 测试也已通过。
整体状态可以认为是：search 模块已经从单纯 Controller 占位推进到应用层 Pipeline 骨架阶段，
但搜索功能本身仍是空响应占位，下一步应继续接入 SearchCollectionResolver、QueryVectorizerSelector、SearchEmbeddingValidator 
等组件，逐步完善真实搜索链路。 */

@Component
public class DefaultSearchPipeline implements SearchPipeline {

    private final Map<SearchType, SearchRequestValidator> validatorMap;
    //搜索请求处理管道的默认实现类，负责执行搜索请求的验证和处理逻辑。它通过构造函数注入一组SearchRequestValidator，并根据搜索类型将它们存储在一个Map中，以便在执行搜索时能够快速找到对应的验证器。

    public DefaultSearchPipeline(List<SearchRequestValidator> validators) {
        this.validatorMap = buildValidatorMap(validators);
    }

    @Override
    /*
    执行搜索请求的核心方法。
    它首先记录请求的开始时间，然后调用validateCommand方法对输入的SearchCommand进行基本的验证（如非空检查和搜索类型检查）。
    接下来，它构建一个SearchContext对象来保存搜索请求的相关信息，并使用getValidator方法根据搜索类型获取对应的SearchRequestValidator进行更详细的验证。
    如果验证失败，则抛出一个BizException异常，包含具体的错误代码。
    最后，它计算整个验证过程的耗时，并返回一个默认的SearchResponse对象。
    */
    public SearchResponse execute(SearchCommand command) {
        long startTime = System.currentTimeMillis();//记录请求开始时间
        validateCommand(command);

        SearchContext context = SearchContext.builder()
                .command(command)
                .searchType(command.getSearchType())
                .topK(command.getTopK())
                .startTime(startTime)
                .build();

        SearchValidateResult validateResult = getValidator(command.getSearchType()).validate(command);
        context.setValidateResult(validateResult);//执行具体的验证逻辑，并将验证结果保存到上下文中

        if (validateResult == null || !validateResult.isValid()) {
            throw new BizException(resolveValidateErrorCode(validateResult));
        }

        long costMs = System.currentTimeMillis() - startTime;
        context.setCostMs(costMs);

        return SearchResponse.builder()
                .searchType(context.getSearchType())
                .total(0)
                .items(Collections.emptyList())
                .costMs(costMs)
                .build();
    //返回一个默认的SearchResponse对象，包含搜索类型、总结果数、结果列表和耗时等信息。注意：实际的搜索结果处理逻辑可以在后续的处理器中实现，这里只是一个示例返回。
    }

    private Map<SearchType, SearchRequestValidator> buildValidatorMap(List<SearchRequestValidator> validators) {
        Map<SearchType, SearchRequestValidator> map = new EnumMap<>(SearchType.class);

        if (validators == null) {
            return map;
        }

        for (SearchRequestValidator validator : validators) {
            if (validator == null) {
                continue;
            }

            SearchType supportType = validator.supportType();
            if (supportType == null) {
                throw new IllegalStateException("SearchRequestValidator supportType must not be null: "
                        + validator.getClass().getName());
            }//确保每个搜索类型只有一个对应的验证器，如果存在重复的验证器，则抛出异常。

            if (map.containsKey(supportType)) {
                throw new IllegalStateException("Duplicate SearchRequestValidator for searchType: " + supportType);
            }//将验证器按照支持的搜索类型存储在一个Map中，以便后续根据搜索类型快速查找对应的验证器。

            map.put(supportType, validator);//将验证器添加到Map中，键为支持的搜索类型，值为验证器实例。
        }

        return map;
    }

    //对输入的SearchCommand进行基本的验证，确保命令对象不为null，并且包含有效的搜索类型。
    private void validateCommand(SearchCommand command) {
        if (command == null) {
            throw new BizException(SearchErrorCode.SEARCH_PARAM_INVALID);
        }

        if (command.getSearchType() == null) {
            throw new BizException(SearchErrorCode.SEARCH_TYPE_INVALID);
        }
    }

    //根据搜索类型获取对应的SearchRequestValidator
    private SearchRequestValidator getValidator(SearchType searchType) {
        SearchRequestValidator validator = validatorMap.get(searchType);
        if (validator == null) {
            throw new BizException(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        return validator;
    }

    //根据验证结果获取对应的错误代码
    private SearchErrorCode resolveValidateErrorCode(SearchValidateResult validateResult) {
        if (validateResult == null || validateResult.getErrorCode() == null) {
            return SearchErrorCode.SEARCH_PARAM_INVALID;
        }

        return validateResult.getErrorCode();
    }
}
