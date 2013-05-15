package de.tudarmstadt.ukp.dkpro.tc.experiments.twentynewsgroups;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createDescription;

import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.TaskBase;
import de.tudarmstadt.ukp.dkpro.tc.core.extractor.SingleLabelInstanceExtractor;
import de.tudarmstadt.ukp.dkpro.tc.core.report.BatchOutcomeReport;
import de.tudarmstadt.ukp.dkpro.tc.core.report.BatchReport;
import de.tudarmstadt.ukp.dkpro.tc.core.report.CVBatchReport;
import de.tudarmstadt.ukp.dkpro.tc.core.report.CVReport;
import de.tudarmstadt.ukp.dkpro.tc.core.report.OutcomeReport;
import de.tudarmstadt.ukp.dkpro.tc.core.report.TestReport;
import de.tudarmstadt.ukp.dkpro.tc.core.task.CrossValidationTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.ExtractFeaturesTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.MetaInfoTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.PreprocessTask;
import de.tudarmstadt.ukp.dkpro.tc.core.task.TestTask;
import de.tudarmstadt.ukp.dkpro.tc.io.TwentyNewsgroupCorpusReader;

public class TwentyNewsgroupsExperiment
{

    static String jsonPath;
    static JSONObject json;

    public static String languageCode;
    public static String corpusFilePathTrain;
    public static String corpusFilePathTest;

    public static void main(String[] args)
        throws Exception
    {

        jsonPath = FileUtils.readFileToString(new File("src/main/resources/config/train.json"));
        json = (JSONObject) JSONSerializer.toJSON(jsonPath);

        languageCode = json.getString("languageCode");
        corpusFilePathTrain = json.getString("corpusFilePathTrain");
        corpusFilePathTest = json.getString("corpusFilePathTest");

        runCrossValidation(ParameterSpaceParser.createParamSpaceFromJson(json));

        runTrainTest(ParameterSpaceParser.createParamSpaceFromJson(json));
    }

    // ##### CV #####
    private static void runCrossValidation(ParameterSpace pSpace)
        throws Exception
    {
        PreprocessTask preprocessTask = new PreprocessTask();
        preprocessTask.setReader(getReaderDesc(corpusFilePathTrain));
        preprocessTask.setAggregate(getPreprocessing(languageCode));
        preprocessTask.setType(preprocessTask.getType() + "-TwentyNewsgroupsCV");

        // get some meta data depending on the whole document collection that we need for training
        TaskBase metaTask = new MetaInfoTask();
        metaTask.setType(metaTask.getType() + "-TwentyNewsgroupsCV");
        metaTask.addImportLatest(MetaInfoTask.INPUT_KEY, PreprocessTask.OUTPUT_KEY,
                preprocessTask.getType());

        // Define the base task which generates an arff instances file
        ExtractFeaturesTask trainTask = new ExtractFeaturesTask();
        trainTask.setAddInstanceId(false);
        trainTask.setInstanceExtractor(SingleLabelInstanceExtractor.class);
        trainTask.setType(trainTask.getType() + "-TwentyNewsgroupsCV");
        trainTask.addImportLatest(MetaInfoTask.INPUT_KEY, PreprocessTask.OUTPUT_KEY,
                preprocessTask.getType());
        trainTask.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaTask.getType());

        // Define the cross-validation task which operates on the results of the the train task
        TaskBase cvTask = new CrossValidationTask();
        cvTask.setType(cvTask.getType() + "TwentyNewsgroupsCV");
        cvTask.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaTask.getType());
        cvTask.addImportLatest(CrossValidationTask.INPUT_KEY, ExtractFeaturesTask.OUTPUT_KEY,
                trainTask.getType());
        cvTask.addReport(CVReport.class);

        // Define the overall task scenario
        BatchTask batch = new BatchTask();
        batch.setType("Evaluation-TwentyNewsgroups-CV");
        batch.setParameterSpace(pSpace);
        batch.addTask(preprocessTask);
        batch.addTask(metaTask);
        batch.addTask(trainTask);
        batch.addTask(cvTask);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(CVBatchReport.class);

        // Run
        Lab.newInstance("/lab/debug_context.xml").run(batch);
    }

    // ##### TRAIN-TEST #####
    private static void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {
        PreprocessTask preprocessTaskTrain = new PreprocessTask();
        preprocessTaskTrain.setReader(getReaderDesc(corpusFilePathTrain));
        preprocessTaskTrain.setAggregate(getPreprocessing(languageCode));
        preprocessTaskTrain.setType(preprocessTaskTrain.getType() + "-TwentyNewsgroups-Train");

        PreprocessTask preprocessTaskTest = new PreprocessTask();
        preprocessTaskTest.setReader(getReaderDesc(corpusFilePathTest));
        preprocessTaskTest.setAggregate(getPreprocessing(languageCode));
        preprocessTaskTrain.setType(preprocessTaskTest.getType() + "-TwentyNewsgroups-Test");

        // get some meta data depending on the whole document collection that we need for training
        TaskBase metaTask = new MetaInfoTask();
        metaTask.setType(metaTask.getType() + "-TwentyNewsgroups");
        metaTask.addImportLatest(MetaInfoTask.INPUT_KEY, PreprocessTask.OUTPUT_KEY,
                preprocessTaskTrain.getType());

        ExtractFeaturesTask featuresTrainTask = new ExtractFeaturesTask();
        featuresTrainTask.setAddInstanceId(true);
        featuresTrainTask.setInstanceExtractor(SingleLabelInstanceExtractor.class);
        featuresTrainTask.setType(featuresTrainTask.getType() + "-TwentyNewsgroups-Train");
        featuresTrainTask.addImportLatest(MetaInfoTask.INPUT_KEY, PreprocessTask.OUTPUT_KEY,
                preprocessTaskTrain.getType());
        featuresTrainTask.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY,
                metaTask.getType());

        ExtractFeaturesTask featuresTestTask = new ExtractFeaturesTask();
        featuresTestTask.setAddInstanceId(true);
        featuresTestTask.setInstanceExtractor(SingleLabelInstanceExtractor.class);
                featuresTestTask.setType(featuresTestTask.getType() + "TwentyNewsgroups-Test");
        featuresTestTask.addImportLatest(MetaInfoTask.INPUT_KEY, PreprocessTask.OUTPUT_KEY,
                preprocessTaskTest.getType());
        featuresTestTask.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY,
                metaTask.getType());

        // Define the test task which operates on the results of the the train task
        TaskBase testTask = new TestTask();
        testTask.setType(testTask.getType() + "-TwentyNewsgroups");
        testTask.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaTask.getType());
        testTask.addImportLatest(TestTask.INPUT_KEY_TRAIN, ExtractFeaturesTask.OUTPUT_KEY,
                featuresTrainTask.getType());
        testTask.addImportLatest(TestTask.INPUT_KEY_TEST, ExtractFeaturesTask.OUTPUT_KEY,
                featuresTestTask.getType());
        testTask.addReport(TestReport.class);
        testTask.addReport(OutcomeReport.class);

        // Define the overall task scenario
        BatchTask batch = new BatchTask();
        batch.setType("Evaluation-TwentyNewsgroups-TrainTest");
        batch.setParameterSpace(pSpace);
        batch.addTask(preprocessTaskTrain);
        batch.addTask(preprocessTaskTest);
        batch.addTask(metaTask);
        batch.addTask(featuresTrainTask);
        batch.addTask(featuresTestTask);
        batch.addTask(testTask);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        batch.addReport(BatchReport.class);
        batch.addReport(BatchOutcomeReport.class);

        // Run
        Lab.newInstance("/lab/debug_context.xml").run(batch);
    }

    private static CollectionReaderDescription getReaderDesc(String corpusFilePath)
        throws ResourceInitializationException, IOException
    {

        return createDescription(TwentyNewsgroupCorpusReader.class,
                TwentyNewsgroupCorpusReader.PARAM_PATH, corpusFilePath,
                TwentyNewsgroupCorpusReader.PARAM_PATTERNS,
                new String[] { TwentyNewsgroupCorpusReader.INCLUDE_PREFIX + "*/*.txt" });
    }

    public static AnalysisEngineDescription getPreprocessing(String languageCode)
        throws ResourceInitializationException
    {

        return createAggregateDescription(
                createPrimitiveDescription(BreakIteratorSegmenter.class),
                createPrimitiveDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        languageCode));
    }
}
