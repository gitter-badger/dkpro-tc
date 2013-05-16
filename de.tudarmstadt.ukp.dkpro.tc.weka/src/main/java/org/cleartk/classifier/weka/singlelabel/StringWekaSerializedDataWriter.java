package org.cleartk.classifier.weka.singlelabel;

import java.io.File;
import java.io.IOException;

import org.cleartk.classifier.CleartkProcessingException;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.encoder.outcome.StringToStringOutcomeEncoder;

public class StringWekaSerializedDataWriter
    extends AbstractSerializedWekaDataWriter<String>
{

    public StringWekaSerializedDataWriter(File outputDirectory)
        throws IOException
    {
        super(outputDirectory);
        this.setOutcomeEncoder(new StringToStringOutcomeEncoder());
    }

    public StringWekaSerializedDataWriter(File outputDirectory, String relationTag)
        throws IOException
    {
        super(outputDirectory, relationTag);
        this.setOutcomeEncoder(new StringToStringOutcomeEncoder());
    }

    @Override
    protected StringWekaClassifierBuilder newClassifierBuilder()
    {
        return new StringWekaClassifierBuilder();
    }

    @Override
    public void write(Instance<String> instance)
        throws CleartkProcessingException
    {
        super.write(instance);
    }

}