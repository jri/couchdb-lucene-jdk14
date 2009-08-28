package com.github.rnewson.couchdb.lucene;

/**
 * Copyright 2009 Robert Newson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.rnewson.couchdb.lucene.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.nutch.analysis.lang.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParsingReader;

public final class Tika {

    private static final Logger log = LogManager.getLogger(Tika.class);

    private static final String DC = "_dc.";

    public void parse(final InputStream in, final String contentType, final String fieldName, final Document doc)
            throws IOException {
        final AutoDetectParser parser = new AutoDetectParser();
        final Metadata md = new Metadata();
        md.set(Metadata.CONTENT_TYPE, contentType);

        final Reader reader = new ParsingReader(parser, in, md);
        final String body;
        try {
            try {
                body = IOUtils.toString(reader);
            } finally {
                reader.close();
            }
        } catch (final IOException e) {
            log.warn("Failed to index an attachment.", e);
            return;
        }

        // Add body text.
        doc.add(Utils.text(fieldName, body, false));

        // Add DC attributes.
        addDublinCoreAttributes(md, doc);

        // Detect language.
        final String language = LanguageIdentifier.identifyLanguage(body);
        if (language != null && language.length() > 0)
            doc.add(Utils.text(DC + DublinCore.LANGUAGE, language, false));
    }

    private void addDublinCoreAttributes(final Metadata md, final Document doc) {
        addAttribute(DC, DublinCore.CONTRIBUTOR, md, doc);
        addAttribute(DC, DublinCore.COVERAGE, md, doc);
        addAttribute(DC, DublinCore.CREATOR, md, doc);
        addAttribute(DC, DublinCore.DATE, md, doc);
        addAttribute(DC, DublinCore.DESCRIPTION, md, doc);
        addAttribute(DC, DublinCore.FORMAT, md, doc);
        addAttribute(DC, DublinCore.IDENTIFIER, md, doc);
        addAttribute(DC, DublinCore.LANGUAGE, md, doc);
        addAttribute(DC, DublinCore.MODIFIED, md, doc);
        addAttribute(DC, DublinCore.PUBLISHER, md, doc);
        addAttribute(DC, DublinCore.RELATION, md, doc);
        addAttribute(DC, DublinCore.RIGHTS, md, doc);
        addAttribute(DC, DublinCore.SOURCE, md, doc);
        addAttribute(DC, DublinCore.SUBJECT, md, doc);
        addAttribute(DC, DublinCore.TITLE, md, doc);
        addAttribute(DC, DublinCore.TYPE, md, doc);
    }

    private void addAttribute(final String namespace, final String attributeName, final Metadata md, final Document doc) {
        if (md.get(attributeName) != null) {
            doc.add(Utils.text(namespace + attributeName, md.get(attributeName), false));
        }
    }
}
