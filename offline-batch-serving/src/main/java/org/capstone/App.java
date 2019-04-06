package org.capstone;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.DefaultVisitorBattery;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.filters.ImportFilter;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

//import jdk.internal.org.xml.sax.InputSource;
//import jdk.internal.org.xml.sax.SAXException;

/**
 * Hello world!
 *
 */
public class App 
{

	
    public static void main( String[] args )
    {
        
     // Building a model evaluator from a PMML file
        Evaluator evaluator;
		try {
			evaluator = new LoadingModelEvaluatorBuilder()
				.setLocatable(false)
				.setVisitors(new DefaultVisitorBattery())
				//.setOutputFilter(OutputFilters.KEEP_FINAL_RESULTS)
				.load(new File("Model.pmml"))
				.build();
		

        // Perforing the self-check
        evaluator.verify();

        // Printing input (x1, x2, .., xn) fields
        List<? extends InputField> inputFields = evaluator.getInputFields();
        System.out.println("Input fields: " + inputFields);

        // Printing primary result (y) field(s)
        List<? extends TargetField> targetFields = evaluator.getTargetFields();
        System.out.println("Target field(s): " + targetFields);

        // Printing secondary result (eg. probability(y), decision(y)) fields
        List<? extends OutputField> outputFields = evaluator.getOutputFields();
        System.out.println("Output fields: " + outputFields);

        // Iterating through columnar data (eg. a CSV file, an SQL result set)
       // while(true){
        	// Reading a record from the data source
//        	Map<String, ?> inputRecord = readRecord();
//        	if(inputRecord == null){
//        		break;
//        	}

        
      
            Map<String, Double> inputRecord = new LinkedHashMap<>();
            
            inputRecord.put("SepalLengthCm", 5.1);
            inputRecord.put("SepalWidthCm", 3.5);
            inputRecord.put("PetalLengthCm", 1.4);
            inputRecord.put("PetalWidthCm", 0.2);
            
            Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        	// Mapping the record field-by-field from data source schema to PMML schema
        	for(InputField inputField : inputFields){
        		FieldName inputName = inputField.getName();

        		Object rawValue = inputRecord.get(inputName.getValue());

        		// Transforming an arbitrary user-supplied value to a known-good PMML value
        		FieldValue inputValue = inputField.prepare(rawValue);

        		arguments.put(inputName, inputValue);
        	}

        	// Evaluating the model with known arguments
        	Map<FieldName, ?> results = evaluator.evaluate(arguments);

        	// Decoupling results from the JPMML-Evaluator runtime environment
        	Map<String, ?> resultRecord = EvaluatorUtil.decodeAll(results);

        	// print the output of the model
        	System.out.println((resultRecord));
      //  }

        // Making the model evaluator eligible for garbage collection
        evaluator = null;
		} catch (IOException | org.xml.sax.SAXException | JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
