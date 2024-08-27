/*
 **  Elaraby_TimesheetInfo_mxJPO
 **
 **  Custom java program to extract timesheet data
 **  This version of the file includes two methods (extractIsolatedEffortsTimesheetData() && extractIsolatedTasksData()), the first extracts the timesheets' data that contain isolated efforts, the second extracts the timesheets' data that contain efforts connected to standalone tasks (without projects)
 */

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.program.ProgramCentralUtil;
import com.matrixone.apps.program.WeeklyTimesheet;
import com.matrixone.apps.program.Task;
import com.matrixone.apps.program.ProjectSpace;
import com.matrixone.apps.program.ProgramCentralConstants;

public class Elaraby_TimesheetInfo_mxJPO {

	public Elaraby_TimesheetInfo_mxJPO() throws Exception {
		super();
    }

	public void extractIsolatedEffortsTimesheetData(Context context, String[] args) throws Exception {
		System.out.println("Executing the method extractTimesheetData...");



		StringList objSelects = new StringList(DomainConstants.SELECT_ID);
		objSelects.add(DomainConstants.SELECT_NAME);

		String sWhere = "";
		//String sWhere = "current==Submit";
		MapList mlTimesheet = DomainObject.findObjects(context, "Weekly Timesheet", "*","*", "*", "*", sWhere, true, objSelects);

		//Karim -- START
		String shasEffort_relationship = PropertyUtil.getSchemaProperty(context,"relationship_hasEfforts");
		String timesheetName = new String();
		String timesheetOwnerName = new String();
		String timesheetState = new String();
		String timesheetRevision = new String();
		String timesheetId = new String();
		// HashMap<String, String> isolatedInfo = new HashMap<String, String>();
		String[] headers = {"Name", "Revision", "State", "Owner", "ID"};
		String[] data = new String[5];
		File csvOutputFile = new File("C:\\temp\\Timesheet_Info.csv");
		PrintWriter printWriter = new PrintWriter(csvOutputFile);

		for(String s : headers){
			printWriter.print(s);
			if(s != "ID"){
				printWriter.print(",");
			}
		}
		printWriter.print("\n");

		// printwriter.flush();
		//Karim -- END



		int modSize = mlTimesheet.size();
		System.out.println("Total No.s of Timesheet==>     " + modSize);

		PrintStream output = new PrintStream(new File("C:\\temp\\log.txt"));
		PrintStream console = System.out;
		System.setOut(output);

		for (int i = 0; i < modSize; i++) {
			MapList mlEfforts = new MapList();

			String objectId = (String) (((Map) mlTimesheet.get(i)).get(DomainConstants.SELECT_ID));
			DomainObject domObj = DomainObject.newInstance(context, objectId);
			System.out.println("Timesheet ID==>     " + objectId + "  ,   Row No==>     " + i);

			if(domObj.isKindOf(context, "Weekly Timesheet")) {
				WeeklyTimesheet weeklyTimesheet = new WeeklyTimesheet(objectId);

				ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"),DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

				try {
					mlEfforts = weeklyTimesheet.getEfforts(context, null, null, null);
				} finally {
					ContextUtil.popContext(context);
				}

				int effSize = mlEfforts.size();
				System.out.println("Total No.s of Efforts==>     " + effSize);
				for (int j = 0; j < effSize; j++) {
					String effortId = (String) (((Map) mlEfforts.get(j)).get(DomainConstants.SELECT_ID));
					//System.out.println("Effort ID==>     " + effortId);

					//Karim -- START
					Map mEffort = new HashMap();
					mEffort = (Map)mlEfforts.get(j);
					String strTaskId = (String)mEffort.get("to["+shasEffort_relationship+"].from.id");
					if(ProgramCentralUtil.isNullString(strTaskId)){
						System.out.println("\n" + "Effort ID: " + effortId + "  ** AN ISOLATED EFFORT **" + "\n");
						timesheetName = (String)domObj.getInfo(context, DomainConstants.SELECT_NAME);
						timesheetState = (String)domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
						timesheetRevision = (String)domObj.getInfo(context, DomainConstants.SELECT_REVISION);
						timesheetOwnerName = (String)domObj.getInfo(context, DomainConstants.SELECT_OWNER);
						timesheetId = (String)domObj.getInfo(context, DomainConstants.SELECT_ID);

						data[0] = timesheetName;
						data[1] = timesheetRevision;
						data[2] = timesheetState;
						data[3] = timesheetOwnerName;
						data[4] = timesheetId;
						//
						// isolatedInfo.put("Timesheet Name", timesheetName);
						// isolatedInfo.put("Timesheet State", timesheetState);
						// isolatedInfo.put("Timesheet Revision", timesheetRevision);
						// isolatedInfo.put("Timesheet Owner", timesheetOwnerName);
						// isolatedInfo.put("Timesheet ID", timesheetId);

						for(String s : data){
							printWriter.print(s);
							if(s != timesheetId){
								printWriter.print(",");
							}
						}

						//
						// for(String iterator : isolatedInfo.keySet()){
						// 	printWriter.print(iterator);
						// 	printWriter.print(": ");
						// 	printWriter.print(isolatedInfo.get(iterator));
						// 	printWriter.print(" , ");
						// }

						printWriter.print("\n");
					}

					try {
						DomainObject domEff = DomainObject.newInstance(context, effortId);
						System.out.println("SUCCESSS: Effort ID==>     " + effortId);
					} catch (Exception e) {
						System.out.println("FAILURE: Effort ID==>     " + effortId);
						System.out.println("Error Message: " + e.getMessage());
					}
				}
			}
		}

		printWriter.flush();
		printWriter.close();
		//Karim -- END
		System.setOut(console);
		System.out.println("Successfully executed the method extractTimesheetData");
	}

	public void extractIsolatedTasksData(Context context, String[] args) throws Exception {
		System.out.println("Executing the method extractIsolatedTasksData...");



		StringList objSelects = new StringList(DomainConstants.SELECT_ID);
		objSelects.add(DomainConstants.SELECT_NAME);

		String sWhere = "";
		//String sWhere = "current==Submit";
		MapList mlTimesheet = DomainObject.findObjects(context, "Weekly Timesheet", "*","*", "*", "*", sWhere, true, objSelects);

		//Karim -- START
		String shasEffort_relationship = PropertyUtil.getSchemaProperty(context,"relationship_hasEfforts");
		String timesheetName = new String();
		String timesheetOwnerName = new String();
		String timesheetState = new String();
		String timesheetRevision = new String();
		String timesheetId = new String();
		String[] headers = {"Timesheet Name", "Timesheet State", "Timesheet Revision", "Timesheet Owner", "Timesheet ID"};
		String[] data = new String[5];
		File csvOutputFile = new File("C:\\temp\\Timesheet_TasksWithoutProject_Info.csv");
		PrintWriter printWriter = new PrintWriter(csvOutputFile);

		for(String s : headers){
			printWriter.print(s);
			if(s != "Timesheet ID"){
				printWriter.print(", ");
			}
		}
		printWriter.print("\n");
		//Karim -- END



		int modSize = mlTimesheet.size();
		System.out.println("Total No.s of Timesheet==>     " + modSize);

		PrintStream output = new PrintStream(new File("C:\\temp\\log.txt"));
		PrintStream console = System.out;
		System.setOut(output);

		for (int i = 0; i < modSize; i++) {
			MapList mlEfforts = new MapList();

			String objectId = (String) (((Map) mlTimesheet.get(i)).get(DomainConstants.SELECT_ID));
			DomainObject domObj = DomainObject.newInstance(context, objectId);
			System.out.println("Timesheet ID==>     " + objectId + "  ,   Row No==>     " + i);

			if(domObj.isKindOf(context, "Weekly Timesheet")) {
				WeeklyTimesheet weeklyTimesheet = new WeeklyTimesheet(objectId);

				ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"),DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

				try {
					mlEfforts = weeklyTimesheet.getEfforts(context, null, null, null);
				} finally {
					ContextUtil.popContext(context);
				}

				int effSize = mlEfforts.size();
				System.out.println("Total No.s of Efforts==>     " + effSize);
				for (int j = 0; j < effSize; j++) {
					String effortId = (String) (((Map) mlEfforts.get(j)).get(DomainConstants.SELECT_ID));
					//System.out.println("Effort ID==>     " + effortId);

					//Karim -- START
					Map mEffort = new HashMap();
					mEffort = (Map)mlEfforts.get(j);
					String strTaskId = (String)mEffort.get("to["+shasEffort_relationship+"].from.id");

					if(ProgramCentralUtil.isNotNullString(strTaskId)){
						Task task = (Task) DomainObject.newInstance(context, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);
						ProjectSpace project = (ProjectSpace) DomainObject.newInstance(context, DomainConstants.TYPE_PROJECT_SPACE, DomainConstants.PROGRAM);
						task.setId(strTaskId);
						ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
						String projectId = task.getInfo(context, ProgramCentralConstants.SELECT_PROJECT_ID);

						if(ProgramCentralUtil.isNotNullString(projectId)){
							timesheetName = (String)domObj.getInfo(context, DomainConstants.SELECT_NAME);
							timesheetState = (String)domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
							timesheetRevision = (String)domObj.getInfo(context, DomainConstants.SELECT_REVISION);
							timesheetOwnerName = (String)domObj.getInfo(context, DomainConstants.SELECT_OWNER);
							timesheetId = (String)domObj.getInfo(context, DomainConstants.SELECT_ID);

							data[0] = timesheetName;
							data[1] = timesheetState;
							data[2] = timesheetRevision;
							data[3] = timesheetOwnerName;
							data[4] = timesheetId;

							for(String s : data){
								printWriter.print(s);
								if(s != timesheetId){
									printWriter.print(", ");
								}
							}

							printWriter.print("\n");

						}
					}

					try {
						DomainObject domEff = DomainObject.newInstance(context, effortId);
						System.out.println("SUCCESSS: Effort ID==>     " + effortId);
					} catch (Exception e) {
						System.out.println("FAILURE: Effort ID==>     " + effortId);
						System.out.println("Error Message: " + e.getMessage());
					}
				}
			}
		}

		printWriter.flush();
		printWriter.close();
		//Karim -- END
		System.setOut(console);
		System.out.println("Successfully executed the method extractIsolatedTasksData");
	}

}
