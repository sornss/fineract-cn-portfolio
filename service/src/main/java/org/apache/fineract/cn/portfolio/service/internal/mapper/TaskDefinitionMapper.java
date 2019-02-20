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
package org.apache.fineract.cn.portfolio.service.internal.mapper;

import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskDefinitionEntity;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
public class TaskDefinitionMapper {
  public static TaskDefinitionEntity map(final ProductEntity productEntity, final TaskDefinition taskDefinition) {
    final TaskDefinitionEntity ret = new TaskDefinitionEntity();
    ret.setIdentifier(taskDefinition.getIdentifier());
    ret.setProduct(productEntity);
    ret.setName(taskDefinition.getName());
    ret.setDescription(taskDefinition.getDescription());
    ret.setMandatory(taskDefinition.getMandatory());
    ret.setFourEyes(taskDefinition.getFourEyes());
    ret.setActions(mapActions(taskDefinition.getActions()));

    return ret;
  }

  public static TaskDefinition map(final TaskDefinitionEntity from) {
    final TaskDefinition ret = new TaskDefinition();

    ret.setIdentifier(from.getIdentifier());
    ret.setName(from.getName());
    ret.setDescription(from.getDescription());
    ret.setActions(mapActions(from));
    ret.setFourEyes(from.getFourEyes());
    ret.setMandatory(from.getMandatory());

    return ret;
  }

  private static String mapActions(final Set<String> actions) {
    return StringUtils.join(actions, ";");
  }

  private static Set<String> mapActions(final TaskDefinitionEntity fromTaskDefinitionEntity) {
    return Arrays.stream(fromTaskDefinitionEntity.getActions().split(";"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
  }
}
