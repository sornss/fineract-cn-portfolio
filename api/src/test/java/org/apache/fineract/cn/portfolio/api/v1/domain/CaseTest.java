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
package org.apache.fineract.cn.portfolio.api.v1.domain;

import org.apache.fineract.cn.Fixture;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
public class CaseTest extends ValidationTest<Case> {

  public CaseTest(ValidationTestCase<Case> testCase) {
    super(testCase);
  }

  @Override
  protected Case createValidTestSubject() {
    return Fixture.getTestCase("blah");
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<Case>("validCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Case>("noIdentifier")
            .adjustment(x -> x.setIdentifier(null))
            .valid(false));
    ret.add(new ValidationTestCase<Case>("tooShortIdentifier")
            .adjustment(x -> x.setIdentifier("b"))
            .valid(false));
    ret.add(new ValidationTestCase<Case>("tooLongIdentifier")
            .adjustment(x -> x.setIdentifier(RandomStringUtils.randomAlphanumeric(33)))
            .valid(false));
    ret.add(new ValidationTestCase<Case>("nonURLSafeIdentifier")
            .adjustment(x -> x.setIdentifier("bad//name"))
            .valid(false));
    ret.add(new ValidationTestCase<Case>("nullProductIdentifier")
            .adjustment(x -> x.setProductIdentifier(null))
            .valid(false));
    ret.add(new ValidationTestCase<Case>("tooLongAccountIdentifier")
        .adjustment(x -> x.getAccountAssignments().add(new AccountAssignment("x", "0123456789")))
        .valid(false));
    ret.add(new ValidationTestCase<Case>("out of range interest")
        .adjustment(x -> x.setInterest(BigDecimal.TEN.negate()))
        .valid(false));
    ret.add(new ValidationTestCase<Case>("null interest")
        .adjustment(x -> x.setInterest(BigDecimal.TEN.negate()))
        .valid(false));

    return ret;
  }
}
