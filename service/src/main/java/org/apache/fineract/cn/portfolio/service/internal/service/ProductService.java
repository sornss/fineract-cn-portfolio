/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.portfolio.service.internal.service;

import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.ProductPage;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ProductMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ConfigurableChargeDefinitionService configurableChargeDefinitionService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public ProductService(final ProductRepository productRepository,
                        final ConfigurableChargeDefinitionService configurableChargeDefinitionService,
                        final AccountingAdapter accountingAdapter) {
    super();
    this.productRepository = productRepository;
    this.configurableChargeDefinitionService = configurableChargeDefinitionService;
    this.accountingAdapter = accountingAdapter;
  }

  public ProductPage findEntities(final boolean includeDisabled,
                                  final @Nullable String term,
                                  final int pageIndex,
                                  final int size,
                                  final @Nullable String sortColumn,
                                  final @Nullable String sortDirection) {
    final Pageable pageRequest = new PageRequest(pageIndex,
            size,
            translateSortDirection(sortDirection),
            translateSortColumn(sortColumn));

    final Page<ProductEntity> ret;
    if (includeDisabled) {
      if (term == null)
        ret = productRepository.findAll(pageRequest);
      else
        ret = productRepository.findByIdentifierContaining(term, pageRequest);
    }
    else {
      if (term == null)
        ret = productRepository.findByEnabled(true, pageRequest);
      else
        ret = productRepository.findByEnabledAndIdentifierContaining(true, term, pageRequest);
    }

    return new ProductPage(ProductMapper.map(ret.getContent()), ret.getTotalPages(), ret.getTotalElements());
  }

  private Sort.Direction translateSortDirection(@Nullable final String sortDirection) {
    return sortDirection == null ? Sort.Direction.DESC :
          Sort.Direction.valueOf(sortDirection);
  }

  private @Nonnull
  String translateSortColumn(@Nullable final String sortColumn) {
    if (sortColumn == null)
      return "lastModifiedOn";

    if (!sortColumn.equals("name") && !sortColumn.equals("identifier") && !sortColumn.equals("lastModifiedOn") && !sortColumn.equals("enabled"))
      throw new IllegalStateException("Illegal input for Sort Column should've been blocked in Rest Controller.");

    return sortColumn;
  }

  public boolean existsByIdentifier(final String identifier)
  {
    //TODO: replace with existsBy once we've upgraded to spring data 1.11 or later.
    return productRepository.findByIdentifier(identifier).isPresent();
  }

  public Optional<Product> findByIdentifier(final String identifier)
  {
    return productRepository.findByIdentifier(identifier).map(ProductMapper::map);
  }

  public Optional<Boolean> findEnabledByIdentifier(final String identifier) {
    return productRepository.findByIdentifier(identifier).map(ProductEntity::getEnabled);
  }

  public Boolean areChargeDefinitionsCoveredByAccountAssignments(final String identifier) {
    final Optional<Product> maybeProduct = findByIdentifier(identifier);
    if (!maybeProduct.isPresent())
      return false;
    final Product product = maybeProduct.get();
    final Set<AccountAssignment> accountAssignments = product.getAccountAssignments();
    final Stream<ChargeDefinition> chargeDefinitions = configurableChargeDefinitionService.findAllEntities(identifier);
    return AccountingAdapter.accountAssignmentsRequiredButNotProvided(accountAssignments, chargeDefinitions).isEmpty();
  }

  public Set<AccountAssignment> getIncompleteAccountAssignments(final String identifier) {
    final Set<String> requiredAccountDesignators = AccountingAdapter.getRequiredAccountDesignators(configurableChargeDefinitionService.findAllEntities(identifier));

    final AccountAssignmentValidator accountAssignmentValidator
            = new AccountAssignmentValidator(findByIdentifier(identifier)
            .map(Product::getAccountAssignments)
            .orElse(Collections.emptySet()));

    return requiredAccountDesignators.stream()
            .map(accountAssignmentValidator::getDesignatorMapping)
            .filter(ProductService::accountAssignmentIsComplete)
            .collect(Collectors.toSet());
  }

  private static boolean accountAssignmentIsComplete(final AccountAssignment x) {
    return (x.getAccountIdentifier() == null) && (x.getLedgerIdentifier() == null);
  }

  private class AccountAssignmentValidator {
    private final Map<String, List<AccountAssignment>> accountAssignmentsMap;

    private AccountAssignmentValidator(final Set<AccountAssignment> accountAssignments)
    {
      accountAssignmentsMap = accountAssignments.stream().collect(Collectors.groupingBy(AccountAssignment::getDesignator));
    }

    private boolean designatorHasValidMapping(final String designator) {
      final List<AccountAssignment> accountAssignment = accountAssignmentsMap.get(designator);

      return ((accountAssignment != null) &&
              (accountAssignment.size() == 1) &&
              accountingAdapter.accountAssignmentRepresentsRealAccount(accountAssignment.get(0)));
    }

    private AccountAssignment getDesignatorMapping(final String designator) {
      if (designatorHasValidMapping(designator))
        return accountAssignmentsMap.get(designator).get(0);
      else
        return new AccountAssignment(designator, null);
    }
  }
}
