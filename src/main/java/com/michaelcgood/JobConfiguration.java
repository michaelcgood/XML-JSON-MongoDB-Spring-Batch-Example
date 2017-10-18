package com.michaelcgood;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


@Configuration
@EnableBatchProcessing
public class JobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        
                        // get path of file in src/main/resources
                        Path xmlDocPath =  Paths.get(getFilePath());
                        
                        // process the file to json
                         String json = processXML2JSON(xmlDocPath);
                         
                         // insert json into mongodb
                         insertToMongo(json);
                        return RepeatStatus.FINISHED;
                    }
                }).build();
    }
    
    public Step step2(){
        return stepBuilderFactory.get("step2")
            .tasklet(new Tasklet(){
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception{
                
                // checks if our collection exists
                Boolean doesexist = mongoTemplate.collectionExists("foo");
                System.out.println("Status of collection returns :::::::::::::::::::::" + doesexist);
                
                // show all DBObjects in foo collection
                DBCursor chowmein = mongoTemplate.getCollection("foo").find();
                List<DBObject> dbarray = chowmein.toArray();
                System.out.println("list of db objects returns:::::::::::::::::::::" + dbarray);
                
                // execute the three methods we defined for querying the foo collection
                String result = doCollect();
                String resultTwo = doCollectTwo();
                String resultThree = doCollectThree();
             
                System.out.println(" RESULT:::::::::::::::::::::" + result);
                
                System.out.println(" RESULT:::::::::::::::::::::" + resultTwo);
                
                System.out.println(" RESULT:::::::::::::::::::::" + resultThree);
                
                
               
                return RepeatStatus.FINISHED;
            }
        }).build();
    }
    
    
    // this will return the id of the policy that has a specific style
    public String doCollect(){
        Query query = new Query();
        query.addCriteria(Criteria.where("Policy.style").is("STY_1.1")).fields().include("Policy.id");
        String result = mongoTemplate.findOne(query, String.class, "foo");
        return result;
    }
    
    // this will return all Value elements (however there is only one).
    public String doCollectTwo(){
        Query query = new Query();
        query.addCriteria(Criteria.where("Policy.style").is("STY_1.1")).fields().include("Policy.Group.Value");
        String result = mongoTemplate.findOne(query, String.class, "foo");
        
        return result;
    }
    
    // 
    
    // searches for policy with specific id and status date. includes only fields title and description within Value element.
    public String doCollectThree(){
        Query query = new Query();
        query.addCriteria(Criteria.where("Policy.id").is("NRD-1").and("Policy.status.date").is("2017-10-18")).fields().include("Policy.Group.Value.title").include("Policy.Group.Value.description");
        String result = mongoTemplate.findOne(query, String.class, "foo");
        
        return result;
    }
    

    // our batch job
    @Bean
    public Job xmlToJsonToMongo() {
        return jobBuilderFactory.get("XML_Processor")
                .start(step1())
                .next(step2())
                .build();
    }

    // takes a parameter of xml path and returns json as a string
    private String processXML2JSON(Path xmlDocPath) throws JSONException {
        
        
        String XML_STRING = null;
        try {
            XML_STRING = Files.lines(xmlDocPath).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        JSONObject xmlJSONObj = XML.toJSONObject(XML_STRING);
        String jsonPrettyPrintString = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
        System.out.println("PRINTING STRING :::::::::::::::::::::" + jsonPrettyPrintString);
        
        return jsonPrettyPrintString;
    }
    
    // no parameter method for creating the path to our xml file
    private String getFilePath(){
        
        String fileName = "FakePolicy.xml";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String xmlFilePath = file.getAbsolutePath();
        
        return xmlFilePath;
    }
    
    // inserts to our mongodb
    private void insertToMongo(String jsonString){
        Document doc = Document.parse(jsonString);
        mongoTemplate.insert(doc, "foo");
    }
}    
