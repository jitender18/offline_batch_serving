package org.capstone;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import javax.xml.bind.JAXBException;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.DefaultVisitorBattery;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


// TODO: Allow for variable input length
// TODO: Substitute hard coded input field name for data header
// TODO: Is it possible to do this without a instantiating a new class?
// TODO: Logging
// TODO: Send to CSV

// Questions?
// 		Broadcasting?
//		Code improvements?
// 		Maintainability?


public class MakePredictions implements Serializable {
	
	private static final long serialVersionUID = 1L;

	static Logger logger = Logger.getLogger(MakePredictions.class);
	
	
	public static void predict(Row row, Evaluator evaluator) throws InterruptedException {

        // Printing input (x1, x2, .., xn) fields
        List<? extends InputField> inputFields = evaluator.getInputFields();
        
        Map<String, Double> inputRecord = new LinkedHashMap<>();

        inputRecord.put("SepalLengthCm", Double.valueOf(row.get(1).toString()));
        inputRecord.put("SepalWidthCm", Double.valueOf(row.get(2).toString()));
        inputRecord.put("PetalLengthCm", Double.valueOf(row.get(3).toString()));
        inputRecord.put("PetalWidthCm", Double.valueOf(row.get(4).toString()));
        
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        
    	// Mapping the record field-by-field from data source schema to PMML schema
    	for(InputField inputField : inputFields){
    		
    		FieldName inputName = inputField.getName();

    		Object rawValue = inputRecord.get(inputName.getValue());

    		// Transforming an arbitrary user-supplied value to a known-good PMML value
    		FieldValue inputValue = inputField.prepare(rawValue);
    		
    		arguments.put(inputName, inputValue);
    	}
    	
		logger.info("Input Parameters: " + arguments.toString() );
		
    	// Evaluating the model with known-good arguments
    	Map<FieldName, ?> results = evaluator.evaluate(arguments);

    	// Decoupling results from the JPMML-Evaluator runtime environment
    	Map<String, ?> resultRecord = EvaluatorUtil.decodeAll(results);

    	// Writing a record to the data sink
    	System.out.println((inputRecord));
    	System.out.println((resultRecord));
    	System.out.println();
    	//Thread.sleep(100);
  //  }
	}
	
	public static void main( String[] args )
    {
		//BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j.properties");

        
     // Building a model evaluator from a PMML file
        Evaluator evaluator;
		try {
			evaluator = new LoadingModelEvaluatorBuilder()
				.setLocatable(false)
				.setVisitors(new DefaultVisitorBattery())
				//.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS)
				.load(new File("Model.pmml"))
				.build();
		
	        // Performing the self-check
	        evaluator.verify();
	
	     
	        // Create spark session
			SparkSession spark = SparkSession.builder().master("local[*]").appName("Spark CSV Reader").getOrCreate();
			
			// Read CSV
			Dataset<Row> data = spark.read()
				    .option("header", true)
				    .csv("Iris.csv");
			
			// Make predictions for each row in CSV
			
	        data.foreach(row -> predict(row, evaluator));
	        
	
	        // Making the model evaluator eligible for garbage collection
	        // evaluator = null;
		} catch (IOException | org.xml.sax.SAXException | JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
