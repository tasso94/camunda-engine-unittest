/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTestCase {

  @RegisterExtension
  ProcessEngineExtension extension = ProcessEngineExtension.builder()
    .build();

  @Test
  public void shouldFail() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${myBean.myMethod(execution.getVariable('foo'), null, 47, execution.getVariable('bar'), execution.getVariable('baz'))}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${false}")
            .endEvent()
        .done();

    extension.getRepositoryService().createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    Mocks.register("myBean", new MyBean());

    // when
    extension.getRuntimeService().startProcessInstanceByKey("testProcess",
        Variables.createVariables()
            .putValue("foo", "a")
            .putValue("bar", null)
            .putValue("baz", "x"));

    // then
    HistoricActivityInstance userTask = extension.getHistoryService().createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();
    assertThat(userTask).isNotNull();
  }

  static class MyBean {

    public boolean myMethod(String v, String w, int x, String y, String z) {
      Logger.getLogger(this.getClass().getName())
          .info(v + w + x + y + z);
      return true;
    }

    public boolean myMethod(String v, String w, int x, String y) {
      return myMethod(v, w, x, y, null);
    }

    public boolean myMethod(String v, String w, int x) {
      return myMethod(v, w, x, null, null);
    }

  }

}
