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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.camunda.bpm.dmn.engine.DmnClause;
import org.camunda.bpm.dmn.engine.DmnClauseEntry;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnRule;
import org.camunda.bpm.dmn.engine.impl.DmnClauseEntryImpl;
import org.camunda.bpm.dmn.engine.impl.DmnClauseImpl;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.camunda.bpm.dmn.engine.impl.DmnItemDefinitionImpl;
import org.camunda.bpm.dmn.engine.impl.DmnRuleImpl;
import org.camunda.bpm.dmn.engine.impl.DmnTypeDefinitionImpl;
import org.camunda.bpm.dmn.engine.impl.type.DefaultDataTypeTransformerFactory;
import org.camunda.bpm.dmn.engine.type.DataTypeTransformerFactory;
import org.camunda.bpm.model.dmn.HitPolicy;

public class DmnHelper {
	/**
	 * a safety pig has been added as a warning ... <code>
	 *                   _
 _._ _..._ .-',     _.._(`))
'-. `     '  /-._.-'    ',/
   )         \            '.
  / _    _    |             \
 |  a    a    /              |
 \   .-.                     ;  
  '-('' ).-'       ,'       ;
     '-;           |      .'
        \           \    /
        | 7  .__  _.-\   \
        | |  |  ``/  /`  /
       /,_|  |   /,_/   /
          /,_/      '`-'
	 * 
	 * </code>
	 */

	private static class Column {
		public boolean input;
		public String displayName;
		public String expression;
		public String type;
		// public String allowedValues;
	}

	private static class Clause {
		Column column;
		String value;
	}

	private static class RowData {
		public List<Clause> conditions = new ArrayList<DmnHelper.Clause>();
		public List<Clause> conclusions = new ArrayList<DmnHelper.Clause>();
	}

	private static volatile long key = 0;
	private final static DataTypeTransformerFactory f = new DefaultDataTypeTransformerFactory();

	private static HitPolicy getHitPolicy(String hitpolicy) {
		Map<String, HitPolicy> hitpolicyMap = new HashMap<String, HitPolicy>();
		hitpolicyMap.put("F", HitPolicy.FIRST);
		hitpolicyMap.put("A", HitPolicy.ANY);
		hitpolicyMap.put("C", HitPolicy.COLLECT);
		hitpolicyMap.put("O", HitPolicy.OUTPUT_ORDER);
		hitpolicyMap.put("P", HitPolicy.PRIORITY);
		hitpolicyMap.put("R", HitPolicy.RULE_ORDER);
		hitpolicyMap.put("U", HitPolicy.UNIQUE);

		return hitpolicyMap.get(hitpolicy);
	}

	private static DmnDecision buildDmnDecision(String tableName,
			String tableHitPolicy, List<Column> columns, List<RowData> rows) {

		DmnDecisionTableImpl dmnDecisionTableImpl = new DmnDecisionTableImpl();
		dmnDecisionTableImpl.setHitPolicy(getHitPolicy(tableHitPolicy));
		dmnDecisionTableImpl.setName(tableName);
		dmnDecisionTableImpl.setKey(createKey());

		Map<String, DmnClause> clauses = new HashMap<String, DmnClause>();
		for (Column column : columns) {
			DmnClause dmnClause;

			if (column.input) {
				dmnClause = createDmnClauseIn(column.displayName,
						column.expression, column.type);
			} else {
				dmnClause = createDmnClauseOut(column.displayName,
						column.expression, column.type);
			}

			clauses.put(column.displayName, dmnClause);
			dmnDecisionTableImpl.addClause(dmnClause);
		}

		int ruleNum = 0;
		for (RowData rowData : rows) {
			List<DmnClauseEntry> cond = new ArrayList<DmnClauseEntry>();
			for (Clause clause : rowData.conditions) {
				cond.add(createDmnClauseEntry(
						clauses.get(clause.column.displayName), clause.value));
			}

			List<DmnClauseEntry> concl = new ArrayList<DmnClauseEntry>();
			for (Clause clause : rowData.conclusions) {
				concl.add(createDmnClauseEntry(
						clauses.get(clause.column.displayName), clause.value));
			}

			dmnDecisionTableImpl.addRule(createDmnRule(ruleNum, cond, concl));
			ruleNum++;
		}

		return dmnDecisionTableImpl;
	}

	public static DmnDecision parseXlsx(String resourceName) throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(resourceName);

		XSSFWorkbook workBook = null;
		try {
			workBook = new XSSFWorkbook(in);

			XSSFSheet sheet = workBook.getSheetAt(0);

			String tableName = DmnXslx.getTableName(sheet);
			String tableHitPolicy = DmnXslx.getTableHitpolicy(sheet);

			List<Column> columns = DmnHelper.buildColumns(sheet);
			List<RowData> rows = DmnHelper.buildRows(columns, sheet);

			return buildDmnDecision(tableName, tableHitPolicy, columns, rows);
		} finally {
			if (workBook != null) {
				workBook.close();
			}
		}
	}

	private static DmnClauseEntry createDmnClauseEntry(DmnClause clause,
			String expression) {
		DmnClauseEntryImpl dmnClauseEntryImpl = new DmnClauseEntryImpl();
		dmnClauseEntryImpl.setKey(createKey());
		dmnClauseEntryImpl.setExpression(expression);
		dmnClauseEntryImpl.setClause(clause);

		return dmnClauseEntryImpl;
	}

	private static DmnRule createDmnRule(int ruleNum,
			List<DmnClauseEntry> conditions, List<DmnClauseEntry> conclusions) {
		DmnRuleImpl dmnRuleImpl = new DmnRuleImpl();
		dmnRuleImpl.setKey(createKey());
		dmnRuleImpl.setName(String.format("rule-%d", ruleNum));
		dmnRuleImpl.setConditions(conditions);
		dmnRuleImpl.setConclusions(conclusions);

		return dmnRuleImpl;
	}

	private static synchronized String createKey() {
		key++;
		return "key-" + String.valueOf(key);
	}	
	
	private static DmnClause createDmnClauseIn(String name, String expression,
			String typeName, String... allowedValues) {
		DmnClauseImpl dmnClauseImpl = new DmnClauseImpl();
		dmnClauseImpl.setKey(createKey());
		dmnClauseImpl.setName(name);

		DmnExpressionImpl dmnExpressionImpl = new DmnExpressionImpl();
		dmnExpressionImpl.setKey(createKey());
		dmnExpressionImpl.setExpression(expression);

		DmnItemDefinitionImpl dmnItemDefinitionImpl = new DmnItemDefinitionImpl();
		dmnItemDefinitionImpl.setKey(createKey());

		DmnTypeDefinitionImpl dmnTypeDefinitionImpl = new DmnTypeDefinitionImpl();
		dmnTypeDefinitionImpl.setTypeName(typeName);
		dmnTypeDefinitionImpl.setTransformer(f.getTransformerForType(typeName));

		dmnItemDefinitionImpl.setTypeDefinition(dmnTypeDefinitionImpl);
		if (allowedValues != null) {
			for (String allowedValue : allowedValues) {
				DmnExpressionImpl d = new DmnExpressionImpl();
				d.setKey(createKey());
				d.setExpression(allowedValue);

				dmnItemDefinitionImpl.addAllowedValue(d);
			}
		}

		dmnExpressionImpl.setItemDefinition(dmnItemDefinitionImpl);

		dmnClauseImpl.setInputExpression(dmnExpressionImpl);

		return dmnClauseImpl;
	}

	private static DmnClause createDmnClauseOut(String name, String expression,
			String typeName, String... allowedValues) {
		DmnClauseImpl dmnClauseImpl = new DmnClauseImpl();
		dmnClauseImpl.setKey(createKey());
		dmnClauseImpl.setName(name);
		dmnClauseImpl.setOutputName(expression);

		DmnExpressionImpl dmnExpressionImpl = new DmnExpressionImpl();
		dmnExpressionImpl.setKey(createKey());
		dmnExpressionImpl.setExpression(expression);

		DmnItemDefinitionImpl dmnItemDefinitionImpl = new DmnItemDefinitionImpl();
		dmnItemDefinitionImpl.setKey(createKey());

		DmnTypeDefinitionImpl dmnTypeDefinitionImpl = new DmnTypeDefinitionImpl();
		dmnTypeDefinitionImpl.setTypeName(typeName);
		dmnTypeDefinitionImpl.setTransformer(f.getTransformerForType(typeName));

		dmnItemDefinitionImpl.setTypeDefinition(dmnTypeDefinitionImpl);
		if (allowedValues != null) {
			for (String allowedValue : allowedValues) {
				DmnExpressionImpl d = new DmnExpressionImpl();
				d.setKey(createKey());
				d.setExpression(allowedValue);

				dmnItemDefinitionImpl.addAllowedValue(d);
			}
		}

		dmnExpressionImpl.setItemDefinition(dmnItemDefinitionImpl);

		dmnClauseImpl.setOutputDefinition(dmnItemDefinitionImpl);

		return dmnClauseImpl;
	}

	private static List<Column> buildColumns(XSSFSheet sheet) {
		List<Column> columns = new ArrayList<Column>();

		XSSFRow row = sheet.getRow(1);
		int col = 1; // first column after the table name
		for (;;) {
			Cell cell = row.getCell(col, Row.RETURN_BLANK_AS_NULL);
			if (cell == null) {
				break;
			}

			Column column = new Column();
			column.input = DmnXslx.isColumnInput(col, sheet);
			column.displayName = DmnXslx.getColumnName(col, sheet);
			column.expression = DmnXslx.getColumnExpression(col, sheet);
			column.type = DmnXslx.getColumnType(col, sheet);
			// column.allowedValues = DmnXslx.getColumnAllowedValues(col,
			// sheet);

			columns.add(column);
			col++;
		}

		return columns;
	}

	private static Clause buildClause(Map<String, Column> columns, int row,
			int col, XSSFSheet sheet) {
		String columnName = DmnXslx.getColumnName(col, sheet);
		Column column = columns.get(columnName);

		Clause clause = new Clause();
		clause.column = column;
		clause.value = sheet.getRow(row).getCell(col).getStringCellValue();

		return clause;
	}

	private static List<RowData> buildRows(List<Column> columns, XSSFSheet sheet) {
		Map<String, Column> columnsMap = new HashMap<String, Column>();
		for (Column column : columns) {
			columnsMap.put(column.displayName, column);
		}

		List<RowData> rowDatas = new ArrayList<RowData>();

		for (int row = 6; DmnXslx.isRowUsed(row, sheet); row++) {
			RowData rowData = new RowData();

			for (int col = 1; DmnXslx.isColumnUsed(col, sheet); col++) {
				Clause clause = buildClause(columnsMap, row, col, sheet);

				if (clause != null) {
					if (clause.column.input) {
						rowData.conditions.add(clause);
					} else {
						rowData.conclusions.add(clause);
					}
				}
			}

			rowDatas.add(rowData);
		}

		return rowDatas;
	}

}
