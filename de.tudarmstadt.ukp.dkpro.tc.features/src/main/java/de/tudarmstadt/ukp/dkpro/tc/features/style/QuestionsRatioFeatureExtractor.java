package de.tudarmstadt.ukp.dkpro.tc.features.style;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class QuestionsRatioFeatureExtractor
    implements SimpleFeatureExtractor
{

    public static final String FN_QUESTION_RATIO = "QuestionRatio";

    @Override
    public List<Feature> extract(JCas jcas, Annotation focusAnnotation)
        throws CleartkExtractorException
    {

        int nrOfSentences = JCasUtil.select(jcas, Sentence.class).size();
        String text = jcas.getDocumentText();

        Pattern p = Pattern.compile("\\?[^\\?]"); // don't count multiple question marks as multiple
                                                  // questions

        int matches = 0;
        Matcher m = p.matcher(text);
        while (m.find()) {
            matches++;
        }

        double questionRatio = 0.0;
        if (nrOfSentences > 0) {
            questionRatio = (double) matches / nrOfSentences;
        }
        
        return Arrays.asList(new Feature(FN_QUESTION_RATIO, questionRatio));
    }
}