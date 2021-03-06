/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.tc.features.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.ClassificationUnitFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;

public class TokenLookUpTable extends FeatureExtractorResource_ImplBase
		implements ClassificationUnitFeatureExtractor {
	private   String lastSeenDocumentId = "";

	protected   HashMap<Integer, Boolean> idx2SentenceBegin = new HashMap<Integer, Boolean>();
	protected   HashMap<Integer, Boolean> idx2SentenceEnd = new HashMap<Integer, Boolean>();

	protected   HashMap<Integer, Token> begin2Token = new HashMap<Integer, Token>();
	protected   HashMap<Integer, Integer> tokenBegin2Idx = new HashMap<Integer, Integer>();
	protected   HashMap<Integer, Integer> tokenEnd2Idx = new HashMap<Integer, Integer>();
	protected   List<String> tokens = new ArrayList<String>();

	public List<Feature> extract(JCas aView,
			TextClassificationUnit aClassificationUnit)
			throws TextClassificationException {
		if (isTheSameDocument(aView)) {
			return null;
		}
		begin2Token = new HashMap<Integer, Token>();
		tokenBegin2Idx = new HashMap<Integer, Integer>();
		idx2SentenceBegin = new HashMap<Integer, Boolean>();
		idx2SentenceEnd = new HashMap<Integer, Boolean>();
		tokens = new ArrayList<String>();

		int i = 0;
		for (Token t : JCasUtil.select(aView, Token.class)) {
			Integer begin = t.getBegin();
			Integer end = t.getEnd();
			begin2Token.put(begin, t);
			tokenBegin2Idx.put(begin, i);
			tokenEnd2Idx.put(end, i);
			tokens.add(t.getCoveredText());
			i++;
		}
		for (Sentence sentence : JCasUtil.select(aView, Sentence.class)) {
			Integer begin = sentence.getBegin();
			Integer end = sentence.getEnd();
			Integer idxStartToken = tokenBegin2Idx.get(begin);
			Integer idxEndtoken = tokenEnd2Idx.get(end);
			idx2SentenceBegin.put(idxStartToken, true);
			idx2SentenceEnd.put(idxEndtoken, true);
		}
		return null;
	}

	private boolean isTheSameDocument(JCas aView) {
		DocumentMetaData meta = JCasUtil.selectSingle(aView,
				DocumentMetaData.class);
		String currentId = meta.getDocumentId();
		boolean isSame = currentId.equals(lastSeenDocumentId);
		lastSeenDocumentId = currentId;
		return isSame;
	}

}
