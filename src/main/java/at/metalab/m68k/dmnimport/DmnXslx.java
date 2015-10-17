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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class DmnXslx {

	public static String getTableName(XSSFSheet sheet) {
		return sheet.getRow(0).getCell(0).getStringCellValue();
	}

	public static String getTableHitpolicy(XSSFSheet sheet) {
		return sheet.getRow(2).getCell(0).getStringCellValue();
	}

	public static boolean isColumnUsed(int column, XSSFSheet sheet) {
		return sheet.getRow(1).getCell(column, Row.RETURN_BLANK_AS_NULL) != null;
	}

	public static boolean isColumnInput(int column, XSSFSheet sheet) {
		return "IN"
				.equals(sheet.getRow(1).getCell(column).getStringCellValue());
	}

	public static String getColumnName(int column, XSSFSheet sheet) {
		return sheet.getRow(2).getCell(column).getStringCellValue();
	}

	public static String getColumnExpression(int column, XSSFSheet sheet) {
		return sheet.getRow(3).getCell(column).getStringCellValue();
	}

	public static String getColumnType(int column, XSSFSheet sheet) {
		return sheet.getRow(4).getCell(column).getStringCellValue();
	}

	public static String getColumnAllowedValues(int column, XSSFSheet sheet) {
		return sheet.getRow(5).getCell(column).getStringCellValue();
	}

	public static boolean isRowUsed(int row, XSSFSheet sheet) {
		return sheet.getRow(row).getCell(0, Row.RETURN_BLANK_AS_NULL) != null;
	}
}