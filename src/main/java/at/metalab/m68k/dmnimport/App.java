/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package at.metalab.m68k.dmnimport;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.impl.DmnEngineConfigurationImpl;

public class App {
	
	public static void main(String[] args) throws Exception {
		// configure the engine
		DmnEngine dmnEngine = new DmnEngineConfigurationImpl().buildEngine();

		// prepare some inputs
		Map<String, Object> dataB = new HashMap<String, Object>();
		dataB.put("status", "bronze");
		dataB.put("sum", 42d);

		Map<String, Object> dataS1 = new HashMap<String, Object>();
		dataS1.put("status", "silver");
		dataS1.put("sum", 23.1d);

		Map<String, Object> dataS2 = new HashMap<String, Object>();
		dataS2.put("status", "silver");
		dataS2.put("sum", 1337d);

		Map<String, Object> dataG = new HashMap<String, Object>();
		dataG.put("status", "gold");
		dataG.put("sum", 354.12d);

		// parse native dmn file
		System.out.println("> evaluating via native dmn decision");

		DmnDecision nativeDmnDecision = dmnEngine.parseDecision(Thread
				.currentThread().getContextClassLoader()
				.getResourceAsStream("checkOrder.dmn"));

		eval(dmnEngine, nativeDmnDecision, dataB);
		eval(dmnEngine, nativeDmnDecision, dataS1);
		eval(dmnEngine, nativeDmnDecision, dataS2);
		eval(dmnEngine, nativeDmnDecision, dataG);

		// parse xlsx dmn file
		System.out.println();
		System.out.println("> evaluating via xlsx-dmn decision");

		DmnDecision xlsxDmnDecision = DmnHelper.parseXlsx("checkOrder.xlsx");

		eval(dmnEngine, xlsxDmnDecision, dataB);
		eval(dmnEngine, xlsxDmnDecision, dataS1);
		eval(dmnEngine, xlsxDmnDecision, dataS2);
		eval(dmnEngine, xlsxDmnDecision, dataG);
	}

	private static DmnDecisionResult eval(DmnEngine dmnEngine, DmnDecision dmnDecision,
			Map<String, Object> data) {
		DmnDecisionResult result = dmnEngine.evaluate(dmnDecision, data);
		System.out.println(data.toString() + " -> " + result);
		return result;
	}

}
