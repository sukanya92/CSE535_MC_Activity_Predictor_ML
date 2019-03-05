package com.cse535.assignment3_group12;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import umich.cse.yctung.androidlibsvm.LibSVM;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Spinner spinnerActivity;
    TextView textView, textParameters, textAccuracy;
    Button button, buttonGraph, buttonTrain, buttonPredict;
    Sensor sensor;
    SensorManager sensorManager;
    private  boolean killMe = false;
    private boolean killMeTest = false;
    double[][] buffer = new double[20][150];
    double[][] bufferWalk = new double[50][60];
    double[][] bufferRun = new double[50][60];
    double[][] bufferJump = new double[50][60];
    int rowWalk = 0;
    int rowRun = 0;
    int rowJump = 0;
    int rowsFilled = 0;
    int colsFilled = 0;
    int i=0;
    int j=0;
    int testCounter = 0;
    boolean tableFilled = false;
    double xVal = 0.0;
    double yVal = 0.0;
    double zVal = 0.0;
    Handler handler;
    boolean tableCreated = false;
    boolean walkingInserted = false;
    boolean runningInserted = false;
    boolean jumpingInserted = false;
    boolean exportToCSV = false;
    String activitySelected = "";
    String TABLE_NAME = "ActivityData";
    String testData = "";
    boolean allHTMLGenerated = false;
    public static SQLiteDatabase db;


    private svm_parameter parameter;
    private svm_problem data_values;
    private int cross_validation;
    private int nr_fold;
    private double accuracy_value = 0;

    /**
     * Convert String value to float value
     * @param s - String input
     * @return - Double value
     */
    private static double toFloat(String s) {
        double d = Double.valueOf(s).doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }

    /**
     * Convert String input value to integer value
     * @param s - String Input
     * @return - integer output
     */
    private static int toInt(String s)
    {
        return Integer.parseInt(s);
    }

    /**
     * To initialize the SVM parameters
     */
    public void parameters() {
        parameter = new svm_parameter();
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = svm_parameter.RBF;
        parameter.eps = 1e-2;
        parameter.p = 0.1;
        parameter.shrinking = 1;
        parameter.probability = 0;
        parameter.nr_weight = 0;

        parameter.degree = 2;
        parameter.gamma = 0.007;
        parameter.coef0 = 0;
        parameter.nu = 0.5;
        parameter.cache_size = 100;
        parameter.C = 10000;

        parameter.weight_label = new int[0];
        parameter.weight = new double[0];
        cross_validation = 1;
        nr_fold = 3;
    }

    /**
     * Function to supplu data from train dataset to SVM model
     * @throws IOException
     */
    public void set_data() throws IOException {
        Reader is = new InputStreamReader(getAssets().open("database.txt"));
        //InputStream inputStream = openFileInput(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/database.txt");
        //Reader is = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(is);
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while(true)
        {
            String line = br.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            vy.addElement(toFloat(st.nextToken()));
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = toInt(st.nextToken());
                x[j].value = toFloat(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, x[m-1].index);
            vx.addElement(x);
        }

        data_values = new svm_problem();
        data_values.l = vy.size();
        data_values.x = new svm_node[data_values.l][];


        for(int i = 0; i< data_values.l; i++)
            data_values.x[i] = vx.elementAt(i);
        data_values.y = new double[data_values.l];
        for(int i = 0; i< data_values.l; i++)
            data_values.y[i] = vy.elementAt(i);


        br.close();
    }

    /**
     * Function to do cross validation for accuracy
     */
    private void cross_validation() {
        int i;
        int total_correct = 0;
        double[] target = new double[data_values.l];

        svm.svm_cross_validation(data_values, parameter,nr_fold,target);
        total_correct = 0;

        for(i=0; i< data_values.l; i++)
            if(target[i] == data_values.y[i])
                ++total_correct;
        accuracy_value = 100.0*total_correct/ data_values.l;


        Toast.makeText(getBaseContext(), "Cross Validation Accuracy = "+100.0*total_correct/ data_values.l+"%\n", Toast.LENGTH_LONG).show();

    }

    /**
     * Function showing accuracy value and parameters on text views
     */
    private void showAccuracy(){
        textParameters.setText("SVM Classifier\n" +
                "kernel_type = svm_parameter.RBF; - Radial base function Kernel\n" +
                "degree = 2;\n" +
                "gamma = 0.007;\n" +
                "cache_size = 100;\n" +
                "C = 10000;\n" +
                "nr_fold =3;\n\n");


        try {

            parameters();
            set_data();

            String error_msg = svm.svm_check_parameter(data_values, parameter);

            if(error_msg != null)
                Toast.makeText(getBaseContext(), error_msg, Toast.LENGTH_LONG).show();

            if(cross_validation != 0)
                cross_validation();
            else
            {
                svm_model model = svm.svm_train(data_values, parameter);
            }

            textAccuracy.setText("Accuracy %ge: "+ accuracy_value);

        }


        catch(Exception ex) {
            Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        createDatabase();
        spinnerActivity = (Spinner) findViewById(R.id.spinnerActivity);
        textView = (TextView)  findViewById(R.id.textView2);
        textParameters = (TextView)  findViewById(R.id.textViewParameters);
        textAccuracy = (TextView)  findViewById(R.id.textViewAccuracy);
        button = (Button)  findViewById(R.id.button);
        buttonGraph = (Button)  findViewById(R.id.button2);
        buttonTrain = (Button)  findViewById(R.id.buttonTrain);
        buttonPredict = (Button)  findViewById(R.id.buttonPredict);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.array_activity, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivity.setAdapter(adapter);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        /* Onclick event for graph button which shows data in 3d graph */
        buttonGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,GraphActivity.class);
                startActivity(intent);
            }
        });


        /* Onclick event for Start button which initiates recording for train data */
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create table
                activitySelected = spinnerActivity.getSelectedItem().toString();
                createTable();
                killMe = false;
                handler.post(action);
            }
        });

        /* Onclick event for train button which initiates the train activity */
        buttonTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String outputModelPath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/model.txt";
                String commandString = "-t 2 ";
                String dataFilePath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/database.txt";
               try {
                   LibSVM.getInstance().train(commandString + dataFilePath + " " + outputModelPath);
                   Toast.makeText(getApplicationContext(), "SVM Train finished. Model created", Toast.LENGTH_SHORT).show();
               }catch(Exception e){
                   Toast.makeText(getApplicationContext(), "Error!!"+e.getMessage(), Toast.LENGTH_SHORT).show();
               }
                showAccuracy();
            }
        });

        /* Onclick event for predict button which starts collecting test data and expects the user
         to perform physical activity and then after 5 secs it predicts by toasting the activity performed */
        buttonPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                xVal = 0; yVal = 0; zVal = 0; i = 0; j = 0; testCounter=0;
                String activity = spinnerActivity.getSelectedItem().toString();
                if(activity.equalsIgnoreCase("Walking"))
                    activity = "+1 ";
                else if(activity.equalsIgnoreCase("Running"))
                    activity = "+2 ";
                else if(activity.equalsIgnoreCase("Jumping"))
                    activity = "+3 ";
                testData = activity;
                killMeTest = false;
                Log.d("Inside ButtonPredict", "onClick: ");
                handler.post(actionTest);
            }
        });

    }


    /* Runnable to collect accelerometer data for training purpose(20 slots of 5 secs each for Walking, Running, and Jumping */
    Runnable action = new Runnable() {
        @Override
        public void run() {
            if(killMe) {
                Log.d("INSIDE KILL", ": value of j "+j);
                textView.setText("Remaining : 0 Sec");
                        updateInsertTable();
                i=0;j=0;
                return;
            }
            textView.setText("Remaining : "+(100-j*5)+" Sec");
            displayVal();
            handler.postDelayed(this, 100);
        }
    };


    /* Runnable to collect accelerometer data for testing purpose(1 slot of 5 secs of the physical activity for which the SVM predicts */
    Runnable actionTest = new Runnable() {
        @Override
        public void run() {

            if(killMeTest) {
                textView.setText("Remaining : 0 Sec");
                displayPredictedActivity();

                return;
            }
            textView.setText("Remaining : "+(5-testCounter)+" Sec");
            captureData();
            handler.postDelayed(this, 100);
        }
    };

    /**
     * This function uses the created model file and test data and predicts
     */
    private void displayPredictedActivity() {
        try {

            Log.d("Inside displayPredicted", "displayPredictedActivity: ");
            File file = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","testData.txt");
            File fileResult = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","resultData.txt");
            if (file.exists()) {
                try {
                    file.delete();
                    file = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","testData.txt");
                    //File newFileName = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","testData.txt");
                    //file.renameTo(newFileName);
                    //file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (fileResult.exists()) {
                try {
                    fileResult.delete();
                    //file = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","testData.txt");
                    //File newFileName = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","testData.txt");
                    //file.renameTo(newFileName);
                    //file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }



            FileWriter writer = new FileWriter(file);
            testData.trim();
            writer.append(testData);
            writer.flush();
            writer.close();
            String testFilePath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/testData.txt";
            String modelPath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/model.txt";
            String resultPath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/resultData.txt";
            LibSVM.getInstance().predict(testFilePath+" " + modelPath+" " + resultPath);
            String prediction = "";
            if(readFile().contains("1")) prediction = "Walking";
            else if (readFile().contains("2")) prediction = "Running";
            else if(readFile().contains("3")) prediction = "Jumping";

            Toast.makeText(getApplicationContext(),"Prediction: Current Activity is  "+prediction, Toast.LENGTH_LONG).show();
            prediction = "";
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * To read file from sdcard
     * @return
     */
    public String readFile(){
        BufferedReader br = null;
        String everything ="";
        try {
            br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/resultData.txt"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
             everything = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return everything;
    }

    /**
     * Create table - Masster dataset containing 60*150 record
     */
    public void createTable(){
        if(!tableCreated){
                createColumn(db);
                tableCreated = true;
        }else{
            Toast.makeText(getApplicationContext(),"Not creating table as it is already created.",Toast.LENGTH_SHORT);
        }
    }

    /**
     * Creates columns for the database
     * @param db
     */
    public void createColumn(SQLiteDatabase db){
        String query = "";
        String intermediateQuery = "";
            for(int m=1;m<=50;m++){
               intermediateQuery += "X"+m+" float, " + "Y"+m+" float, " + "Z"+m+" float, ";
        }
        intermediateQuery = intermediateQuery.substring(0,intermediateQuery.length()-2);
        Log.d("QUERY STRING", ": "+intermediateQuery);
        query = "create table if not exists "+TABLE_NAME+" (ID integer primary key autoincrement, activity_label varchar(20), "+intermediateQuery+")";
        db.execSQL(query);
    }

    /**
     * Generic function for both update and insert
     */
    public void updateInsertTable(){

        String activity = activitySelected;
        boolean insertedFlag = false;
        switch (activity){
            case "Walking":
                insertedFlag = walkingInserted;
            break;

            case "Running":
                insertedFlag = runningInserted;
            break;

            case "Jumping":
                insertedFlag = jumpingInserted;
            break;
        }

        Log.d("Inside UpdateInsert", ":: "+activity+"::"+insertedFlag);


        if(!insertedFlag){
          //then insert
            try {
                insertValues(activity);
            }catch (Exception e){
                Log.d("INSIDE Insert", ":"+"Error");
            }
        }else{
          //then update
          updateValues(activity);
        }

        try {
            if(walkingInserted && runningInserted && jumpingInserted) {
                exportToCSV();
                generateAllHTML();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * FUnction for inserting data into database
     * @param activity
     */
    public void insertValues(String activity){
        ContentValues values = new ContentValues();
        for(int row=0;row<buffer.length;row++){
            values.put("activity_label",activity);
            for(int col=0;col<50;col++){
                values.put("X"+(col+1),buffer[row][col*3+0]);
                values.put("Y"+(col+1),buffer[row][col*3+1]);
                values.put("Z"+(col+1),buffer[row][col*3+2]);
            }
            db.insert(TABLE_NAME, null, values);
        }
        int rowStart = getActivityStartCount();

        if(activity.equals("Walking")){ walkingInserted = true; rowWalk = rowStart;}
        else if(activity.equals("Running")){ runningInserted = true;  rowRun = rowStart;}
        else if(activity.equals("Jumping")){ jumpingInserted = true;  rowJump = rowStart;}
    }

    public int getActivityStartCount(){
        final String MY_QUERY = "SELECT last_insert_rowid()";
        Cursor cur = db.rawQuery(MY_QUERY, null);
        cur.moveToFirst();
        int ID = cur.getInt(0);
        cur.close();
        return (ID-20+1);
    }

    /**
     * Function to update records in database
     * @param activity
     */
    public void updateValues(String activity){
        int rowStart = 0;
        if(activity.equals("Walking")){ rowStart = rowWalk; }
        else if(activity.equals("Running")){ rowStart = rowRun; }
        else if(activity.equals("Jumping")){ rowStart = rowJump; }
        Log.d("Inside UpdateValues", ": "+rowStart+"  "+rowWalk);
        String Update = "UPDATE " + TABLE_NAME + " SET ";
        try {
            for (int row = 0; row < buffer.length; row++) {
            String inter = "";
            for (int col = 0; col < 50; col++) {
                inter += "X" + (col + 1) + " = " + buffer[row][col * 3 + 0] + ", " + "Y" + (col + 1) + " = " + buffer[row][col * 3 + 1] + ", " + "Z" + (col + 1) + " = " + buffer[row][col * 3 + 2] + ", ";
        }
        inter = inter.substring(0, inter.length() - 2);
        inter = Update + inter + " WHERE ID = " + (rowStart+row);
        db.execSQL(inter);
        }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Function to capture accelerometer data for train data
     */
    private void displayVal() {
        if(i%50 == 0){
            if(i!=0)
                j+=1;
            i = i%50;
            if((j%20==0) && (j!=0)){
                killMe=true;
            }
        }
        if(!killMe)
            try{
                buffer[j][i*3+0] = xVal;
                buffer[j][i*3+1] = yVal;
                buffer[j][i*3+2] = zVal;
            }catch(Exception e){
                Log.e("Error",j+"--"+i);
            }
        i++;
    }

    /**
     * Function to capture accelerometer data for test
     */
    private void captureData(){
        if(i == 50) killMeTest=true;
        if(!killMeTest)
            try{
                 testData+=((i*3)+1)+":"+xVal+" ";
                 testData+=((i*3)+2)+":"+yVal+" ";
                 testData+=((i*3)+3)+":"+zVal+" ";
            }catch(Exception e){
                Log.e("Error",j+"--"+i);
            }
        i++;
        if(i%10 == 0) testCounter++;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
         xVal = event.values[0];
         yVal = event.values[1];
         zVal = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Creating database
     */
    public void createDatabase() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED)) {
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/CSE535_ASSIGNMENT_3");
            deleteRecursive(folder);
            folder.mkdirs();
            db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT_3" + "/Group12.db", null, SQLiteDatabase.CREATE_IF_NECESSARY);
        } else {
            // Permission is missing and must be requested.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET }, 1);
        }
    }

    /**
     * Deleting folder
     * @param fileOrDirectory
     */
    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.exists() && fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/CSE535_ASSIGNMENT_3");
            deleteRecursive(folder);
            folder.mkdirs();
            db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/CSE535_ASSIGNMENT_3" + "/Group12.db", null, SQLiteDatabase.CREATE_IF_NECESSARY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){

        }
    }

    /**
     * Creating buffer file/
     * @throws Exception
     */
    public void exportToCSV() throws Exception{

        //initialize the buffers
        Cursor curCSV = db.rawQuery("SELECT * FROM ActivityData",null);
        int k=-1;
        File file = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3","database.txt");
        FileWriter writer=new FileWriter(file);
        while(curCSV.moveToNext()) {
            k++;
            int m = 1;
            // fill the buffer for SVM Train data
            String output = "";
            String labels = curCSV.getString(1);
            writeToText(curCSV, output, writer);

            if(k<20) {
                for (int i = 0; i < 50; i++) {
                    for (int j = k * 3; j < k * 3 + 3; j++) {
                        m++;
                        Log.d("WALKING", "m : "+m+" :: "+curCSV.getFloat(m));
                        bufferWalk[i][j] = curCSV.getFloat(m);
                    }
                }
            }else if(k>=20 && k<40){
                for (int i = 0; i < 50; i++) {
                    for (int j = (k-20) * 3; j < (k-20) * 3 + 3; j++) {
                        m++;
                        bufferRun[i][j] = curCSV.getFloat(m);
                    }
                }
            }else if(k>=40 && k<60){
                for (int i = 0; i < 50; i++) {
                    for (int j = (k-40) * 3; j < (k-40) * 3 + 3; j++) {
                        m++;
                        bufferJump[i][j] = curCSV.getFloat(m);
                    }
                }
            }

        }
        writer.close();
        exportToCSV = true;

        //Toast.makeText(getApplicationContext(),"Database.txt created",)
        //Create 7 csv files (run, walk, jump, run-walk, walk-jump, run-jump, run-walk-jump)
        /*
        File folder = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/CSE535_ASSIGNMENT_3/CSV");
        deleteRecursive(folder);
        folder.mkdirs();
        File fileWalk = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Walk.csv");
        File fileRun = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Run.csv");
        File fileJump = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Jump.csv");
        File fileWalk_Run = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Walk_Run.csv");
        File fileRun_Jump = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Run_Jump.csv");
        File fileWalk_Jump = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Walk_Jump.csv");
        File fileWalk_Run_Jump = new File(Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3/CSV","Walk_Run_Jump.csv");
        File[] files = {fileWalk, fileRun, fileJump, fileWalk_Run, fileRun_Jump, fileWalk_Jump, fileWalk_Run_Jump};
        String[] cols = null;
        for(int i=0;i<1;i++){
            int m = 0;
            if(i==0) m=6;
            double[][] buffer1 = null;
            double[][] buffer2 = null;
            double[][] buffer3 = null;
            CSVWriter csvWrite = new CSVWriter(new FileWriter(files[i]));
            cols = createCols(i, csvWrite);
            if(i==0) {
                buffer1 = new double[50][60]; buffer1 = bufferWalk;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(buffer1[j]);
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==1){
                buffer2 = new double[50][60]; buffer2 = bufferRun;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(buffer2[j]);
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==2){
                buffer3 = new double[50][60]; buffer3 = bufferJump;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(buffer3[j]);
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==3){
                buffer1 = new double[50][60]; buffer1 = bufferWalk;
                buffer2 = new double[50][60]; buffer2 = bufferRun;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(concatenateArrays(buffer1[j],buffer2[j]));
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==4){
                buffer2 = new double[50][60]; buffer2 = bufferRun;
                buffer3 = new double[50][60]; buffer3 = bufferJump;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(concatenateArrays(buffer2[j],buffer3[j]));
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==5){
                buffer1 = new double[50][60]; buffer1 = bufferWalk;
                buffer3 = new double[50][60]; buffer3 = bufferJump;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(concatenateArrays(buffer1[j],buffer3[j]));
                    csvWrite.writeNext(arrayCurrent);
                }
            }else if(i==6){
                buffer1 = new double[50][60]; buffer1 = bufferWalk;
                buffer2 = new double[50][60]; buffer2 = bufferRun;
                buffer3 = new double[50][60]; buffer3 = bufferJump;
                csvWrite.writeNext(cols);
                for(int j=0;j<50;j++){
                    String[] arrayCurrent = convertStringArray(concatenateArrays(buffer1[j], concatenateArrays(buffer2[j],buffer3[j])));
                    csvWrite.writeNext(arrayCurrent);
                }
            }
            csvWrite.close(); */

    }

    public void writeToText(Cursor cursor, String output, FileWriter writer) throws IOException {
        String label = cursor.getString(1);
        if(label.equalsIgnoreCase("Walking")){
            output = "+1 ";
        }else if(label.equalsIgnoreCase("Running")){
            output = "+2 ";
        }else if(label.equalsIgnoreCase("Jumping")){
            output = "+3 ";
        }
        for(int j = 2;j <= 151;j++)
            output += (j-1)+":"+cursor.getFloat(j)+" ";

        output.trim();
        output += "\n";

        writer.append(output);
        writer.flush();


    }

    /**
     * Generate HTML files for showing 3d graph dynamically after every train data set collection
     */
    public void generateAllHTML(){
        String[] fileNames = {"Walk","Run","Jump","WalkRun","RunJump","WalkJump","WalkRunJump"};
        for(int i =0;i<7;i++){
            //Every loop will generate an HTML file and pushes to SDCard.
            switch (i){
                case 0:
                    generateHtml(bufferWalk,null,null,fileNames[i]);
                break;

                case 1:
                    generateHtml(bufferRun,null,null,fileNames[i]);
                break;

                case 2:
                    generateHtml(bufferJump,null,null,fileNames[i]);
                break;

                case 3:
                    generateHtml(bufferWalk,bufferRun,null,fileNames[i]);
                break;

                case 4:
                    generateHtml(bufferRun,bufferJump,null,fileNames[i]);
                break;

                case 5:
                    generateHtml(bufferWalk,bufferJump,null,fileNames[i]);
                break;

                case 6:
                    generateHtml(bufferWalk,bufferRun,bufferJump,fileNames[i]);
                break;

            }

        }allHTMLGenerated = true;
    }

    public String[] createCols(int i, CSVWriter csvWrite){
        String[] cols = null;
        int colNum = 0;
        if(i<=2) colNum = 20;
        else if(i>2 && i<=5) colNum = 40;
        else colNum = 60;
        int totCol = colNum*3;
        cols = new String[totCol];
        for(int j = 0;j<colNum;j++){
            cols[j*3 + 0] = "x"+(j+1);
            cols[j*3 + 1] = "y"+(j+1);
            cols[j*3 + 2] = "z"+(j+1);
        }
        return cols;
    }

    public double[] concatenateArrays(double[] array1, double[] array2){
        double[] result = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public String[] convertStringArray(double[] array){

        String[] s = new String[array.length];
        for (int i = 0; i < s.length; i++)
            s[i] = String.valueOf(array[i]);
        return s;
    }

    /**
     * Generic function to create respective heml(walk/ run/ walk-run/ walk-run-jump)
     * @param b1
     * @param b2
     * @param b3
     * @param name
     */
    public void generateHtml(double[][] b1,double[][] b2, double[][] b3, String name){

        int traceCount=0;
        if(b2==null && b3==null){
           traceCount = 20;
        }else if(b3==null){
            traceCount = 40;
        }else{
            traceCount = 60;
        }
        String dataArray = "";
        try {

            //define a HTML String Builder
            StringBuilder htmlStringBuilder=new StringBuilder();
            //append html header and title
            htmlStringBuilder.append("<html><head><script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>"
                    +"<script src=\"https://d3js.org/d3.v4.min.js\"></script></head>");
            //append body
            htmlStringBuilder.append("<body>\n");
            htmlStringBuilder.append("<div id=\"myDiv\" style=\"width:100%;height:100%\"></div>\n");
            htmlStringBuilder.append("<script>\n");
            htmlStringBuilder.append("Plotly.d3.csv(\"/data/_3d-line-plot.csv\", function(err, rows){\n");
            htmlStringBuilder.append("function unpack(rows, key) {\n");
            htmlStringBuilder.append("return rows.map(function(row){\n");
            htmlStringBuilder.append("return row[key];\n");
            htmlStringBuilder.append("});}\n");

            //How many times array needs to be created
            for(int i=1;i<=traceCount;i++){
                generateArray(htmlStringBuilder,i,  b1,b2,b3);
            }

            for(int i=1;i<=traceCount;i++){
                generateTrace(htmlStringBuilder,i);
            }
            for(int i=1;i<=traceCount;i++){
                dataArray +="trace"+i+",";
            }
            dataArray = dataArray.substring(0,dataArray.length()-1);
            htmlStringBuilder.append("var data = ["+dataArray+"];\n");
            htmlStringBuilder.append("var layout = {title: '3D Line Plot',autosize: false,width: 500,height: 500,margin: {l: 0,r: 0,b: 0,t: 65},showlegend: false};\n");
            htmlStringBuilder.append("Plotly.newPlot('myDiv', data, layout);});\n");
            htmlStringBuilder.append("</script></body>");

            WriteToFile(htmlStringBuilder.toString(),name+".html");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public  void WriteToFile(String fileContent, String fileName) throws IOException {
        String projectPath = Environment.getExternalStorageDirectory() + "/Android/Data/CSE535_ASSIGNMENT_3";
        String tempFile = projectPath + File.separator+fileName;
        File file = new File(tempFile);
        // if file does exists, then delete and create a new file
        if (file.exists()) {
            try {
                File newFileName = new File(projectPath + File.separator+fileName);
                file.renameTo(newFileName);
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //write to file with OutputStreamWriter
        OutputStream outputStream = new FileOutputStream(file.getAbsoluteFile());
        Writer writer=new OutputStreamWriter(outputStream);
        writer.write(fileContent);
        writer.close();

    }

    /**
     * Generating trace for Plotly graph
     * @param sb
     * @param i
     */
    public static void generateTrace(StringBuilder sb, int i){
        String colorCode ="";
        if(i<=20) colorCode = "#0000FF";
        else if(i>20 && i<=40) colorCode = "#008000";
        else colorCode = "#FF0000";
        sb.append("var trace"+i+" = {x: array"+ i+"[0],y: array"+ i +"[1],z: array"+ i +"[2],mode: 'lines',marker: {color: '"+ colorCode +"',size: 14,symbol: 'circle',line: {color: 'rgb(0,0,0)',width: 0}},line: {color: '"+colorCode+"',width: 2},type: 'scatter3d'};\n");
        //sb.append("var data = [trace"+i+"];\n");
    }

    /**
     * Generate data array for Plotly graph
     * @param sb
     * @param i
     * @param b1
     * @param b2
     * @param b3
     */
    public static void generateArray(StringBuilder sb, int i,  double[][] b1,double[][] b2, double[][] b3 ){
        String data ="";
        String s1=""; String s2=""; String s3="";
        String[] s = {s1,s2,s3};
        String combinedData ="";
        if(b2==null  &&  b3==null){
            //to collect 20 data
            for(int j=0;j<3;j++){
               for(int k=0;k<50;k++){
                   combinedData += b1[k][(i-1)*3+j]+",";
               }
               s[j] = combinedData.substring(0,combinedData.length()-1);
               combinedData = "";
            }
           //data  =  " =  [[1.0,1.5,2.0,2.5],[1.0,1.5,2.0,2.5],[1.0,1.5,2.0,2.5]]\n";
             data  = " =  [[" + s[0] +"],["+ s[1] +"],["+ s[2] +"]]\n"  ;
             sb.append("var array"+i+data);

        }else if(b3==null){
            //to collect 40 data
            int count=0;
            double[][][] buffers = {b1,b2};
            if(i>20) count = 1;
            else count = 0;
            //for(int count =0;count<2;count++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 50; k++) {
                        combinedData += buffers[count][k][((i - 1)%20) * 3 + j] + ",";
                    }
                    s[j] = combinedData.substring(0, combinedData.length() - 1);
                    combinedData = "";
                }
                data = " =  [[" + s[0] + "],[" + s[1] + "],[" + s[2] + "]]\n";
                sb.append("var array" + i + data);
            //}

        }else{
            //to collect 60 data
            int count=0;
            double[][][] buffers = {b1,b2,b3};
            if(i>40) count = 2;
            else if (i>20 && i<=40) count = 1;
            else count =0;
            //for(int count =0;count<3;count++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 50; k++) {
                        combinedData += buffers[count][k][((i - 1)%20) * 3 + j] + ",";
                    }
                    s[j] = combinedData.substring(0, combinedData.length() - 1);
                    combinedData = "";
                }
                data = " =  [[" + s[0] + "],[" + s[1] + "],[" + s[2] + "]]\n";
                sb.append("var array" + i + data);
            //}
        }

    }

}
