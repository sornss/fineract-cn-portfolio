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
package org.apache.fineract.cn.portfolio;

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CreditWorthinessFactor;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CreditWorthinessSnapshot;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ProductParameters;
import org.apache.fineract.cn.portfolio.api.v1.domain.*;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators.*;
import static org.apache.fineract.cn.portfolio.AccountingFixture.*;
import static java.math.BigDecimal.ROUND_HALF_EVEN;

/**
 * @author Myrle Krantz
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class Fixture {
  static final int MINOR_CURRENCY_UNIT_DIGITS = 2;
  static final BigDecimal INTEREST_RATE = BigDecimal.valueOf(10_00, 2);
  static final BigDecimal ACCRUAL_PERIODS = BigDecimal.valueOf(365.2425);
  public static final String CUSTOMER_IDENTIFIER = "alice";

  private static int uniquenessSuffix = 0;

  static public Product getTestProduct() {
    final Product product = new Product();
    product.setPatternPackage("org.apache.fineract.cn.individuallending.api.v1");
    product.setIdentifier(generateUniqueIdentifer("agro"));
    product.setName("Agricultural Loan");
    product.setDescription("Loan for seeds or agricultural equipment");
    product.setTermRange(new TermRange(ChronoUnit.MONTHS, 12));
    product.setBalanceRange(new BalanceRange(fixScale(BigDecimal.ZERO), fixScale(new BigDecimal(10_000))));
    product.setInterestRange(new InterestRange(BigDecimal.valueOf(3_00, 2), BigDecimal.valueOf(12_00, 2)));
    product.setInterestBasis(InterestBasis.CURRENT_BALANCE);

    product.setCurrencyCode("XXX");
    product.setMinorCurrencyUnitDigits(MINOR_CURRENCY_UNIT_DIGITS);

    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    accountAssignments.add(new AccountAssignment(PROCESSING_FEE_INCOME, PROCESSING_FEE_INCOME_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(ORIGINATION_FEE_INCOME, LOAN_ORIGINATION_FEES_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(DISBURSEMENT_FEE_INCOME, DISBURSEMENT_FEE_INCOME_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(INTEREST_INCOME, CONSUMER_LOAN_INTEREST_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(INTEREST_ACCRUAL, LOAN_INTEREST_ACCRUAL_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(LATE_FEE_INCOME, LATE_FEE_INCOME_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(LATE_FEE_ACCRUAL, LATE_FEE_ACCRUAL_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(PRODUCT_LOSS_ALLOWANCE, PRODUCT_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER));
    accountAssignments.add(new AccountAssignment(GENERAL_LOSS_ALLOWANCE, GENERAL_LOSS_ALLOWANCE_ACCOUNT_IDENTIFIER));
    //accountAssignments.add(new AccountAssignment(EXPENSE, ...));
    //accountAssignments.add(new AccountAssignment(ENTRY, ...));
    // Don't assign entry and expense accounts in test since they usually will not be assigned IRL.
    accountAssignments.add(new AccountAssignment(LOAN_FUNDS_SOURCE, LOAN_FUNDS_SOURCE_ACCOUNT_IDENTIFIER));
    final AccountAssignment customerLoanPrincipalAccountAssignment = new AccountAssignment();
    customerLoanPrincipalAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    customerLoanPrincipalAccountAssignment.setLedgerIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    accountAssignments.add(customerLoanPrincipalAccountAssignment);

    final AccountAssignment customerLoanInterestAccountAssignment = new AccountAssignment();
    customerLoanInterestAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    customerLoanInterestAccountAssignment.setLedgerIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    accountAssignments.add(customerLoanInterestAccountAssignment);

    final AccountAssignment customerLoanFeesAccountAssignment = new AccountAssignment();
    customerLoanFeesAccountAssignment.setDesignator(AccountDesignators.CUSTOMER_LOAN_FEES);
    customerLoanFeesAccountAssignment.setLedgerIdentifier(CUSTOMER_LOAN_LEDGER_IDENTIFIER);
    accountAssignments.add(customerLoanFeesAccountAssignment);
    product.setAccountAssignments(accountAssignments);

    final ProductParameters productParameters = new ProductParameters();

    productParameters.setMoratoriums(Collections.emptyList());
    productParameters.setMaximumDispersalCount(5);

    final Gson gson = new Gson();
    product.setParameters(gson.toJson(productParameters));
    return product;
  }

  static public Product createAdjustedProduct(final Consumer<Product> adjustment) {
    final Product product = Fixture.getTestProduct();
    adjustment.accept(product);
    return product;
  }

  static public String generateUniqueIdentifer(final String prefix) {
    //prefix followed by a random positive number with less than 4 digits.
    return prefix + (uniquenessSuffix++);
  }

  static public BigDecimal fixScale(final BigDecimal bigDecimal)
  {
    return bigDecimal.setScale(MINOR_CURRENCY_UNIT_DIGITS, ROUND_HALF_EVEN);
  }

  static public Case getTestCase(final String productIdentifier) {
    final Case ret = new Case();

    ret.setIdentifier(generateUniqueIdentifer("loan"));
    ret.setProductIdentifier(productIdentifier);


    final Set<AccountAssignment> accountAssignments = new HashSet<>();
    ret.setAccountAssignments(accountAssignments);
    ret.setCurrentState(Case.State.CREATED.name());
    ret.setInterest(INTEREST_RATE);

    final CaseParameters caseParameters = getTestCaseParameters();
    final Gson gson = new Gson();
    ret.setParameters(gson.toJson(caseParameters));

    return ret;
  }

  static public Case createAdjustedCase(final String productIdentifier, final Consumer<Case> adjustment) {
    final Case ret = Fixture.getTestCase(productIdentifier);
    adjustment.accept(ret);
    return ret;
  }

  static public CaseParameters getTestCaseParameters()
  {
    final CaseParameters ret = new CaseParameters(generateUniqueIdentifer("fred"));

    ret.setCustomerIdentifier(CUSTOMER_IDENTIFIER);
    ret.setMaximumBalance(fixScale(BigDecimal.valueOf(2000L)));
    ret.setTermRange(new TermRange(ChronoUnit.MONTHS, 3));
    ret.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 1, null, null));

    final CreditWorthinessSnapshot customerCreditWorthinessSnapshot = new CreditWorthinessSnapshot();
    customerCreditWorthinessSnapshot.setForCustomer("alice");
    customerCreditWorthinessSnapshot.setDebts(Collections.singletonList(new CreditWorthinessFactor("some debt", fixScale(BigDecimal.valueOf(300)))));
    customerCreditWorthinessSnapshot.setAssets(Collections.singletonList(new CreditWorthinessFactor("some asset", fixScale(BigDecimal.valueOf(500)))));
    customerCreditWorthinessSnapshot.setIncomeSources(Collections.singletonList(new CreditWorthinessFactor("some income source", fixScale(BigDecimal.valueOf(300)))));

    final CreditWorthinessSnapshot cosignerCreditWorthinessSnapshot = new CreditWorthinessSnapshot();
    cosignerCreditWorthinessSnapshot.setForCustomer("seema");
    cosignerCreditWorthinessSnapshot.setDebts(Collections.emptyList());
    cosignerCreditWorthinessSnapshot.setAssets(Collections.singletonList(new CreditWorthinessFactor("a house", fixScale(BigDecimal.valueOf(50000)))));
    cosignerCreditWorthinessSnapshot.setIncomeSources(Collections.singletonList(new CreditWorthinessFactor("retirement", fixScale(BigDecimal.valueOf(200)))));

    final List<CreditWorthinessSnapshot> creditWorthinessSnapshots = new ArrayList<>();
    creditWorthinessSnapshots.add(customerCreditWorthinessSnapshot);
    creditWorthinessSnapshots.add(cosignerCreditWorthinessSnapshot);

    ret.setCreditWorthinessSnapshots(creditWorthinessSnapshots);

    return ret;
  }

  static public CaseParameters createAdjustedCaseParameters(final Consumer<CaseParameters> adjustment) {
    final CaseParameters ret = Fixture.getTestCaseParameters();
    adjustment.accept(ret);
    return ret;
  }
}
