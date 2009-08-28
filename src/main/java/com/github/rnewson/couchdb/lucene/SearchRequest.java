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

import java.lang.Math;
import java.lang.Math;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

public final class SearchRequest {

    private static final Database DB = new Database(Config.DB_URL);

    private static final char DOUBLE_QUOTE = '"';

    private final String dbname;

    private final String viewname;

    private final String viewsig;

    private final Query q;

    private Filter filter;

    private final int skip;

    private final int limit;

    private final Sort sort;

    private final boolean debug;

    private final boolean include_docs;

    private final boolean rewrite_query;

    private final String callback;

    private final String ifNoneMatch;

    private final String contentType;

    public SearchRequest(final JSONObject obj, final String viewsig) throws ParseException {
        final JSONObject headers = obj.getJSONObject("headers");
        final JSONObject query = obj.getJSONObject("query");
        final JSONArray path = obj.getJSONArray("path");

        this.ifNoneMatch = headers.optString("If-None-Match");
        this.dbname = path.getString(0);
        this.viewname = Utils.viewname(path);
        this.viewsig = viewsig;
        this.skip = query.optInt("skip", 0);
        this.limit = query.optInt("limit", 25);
        this.debug = query.optBoolean("debug", false);
        this.include_docs = query.optBoolean("include_docs", false);
        this.rewrite_query = query.optBoolean("rewrite", false);
        this.callback = query.optString("callback", null);

        // Negotiate Content-Type of response.
        if (query.optBoolean("force_json", false) || headers.optString("Accept").indexOf("application/json") != -1) {
            this.contentType = "application/json";
        } else {
            this.contentType = "text/plain;charset=utf-8";
        }

        // Parse query.
        final Analyzer analyzer = AnalyzerCache.getAnalyzer(query.optString("analyzer", "standard"));
        final QueryParser parser = new QueryParser(Config.DEFAULT_FIELD, analyzer);
        if ("AND".equalsIgnoreCase(Config.DEFAULT_OPERATOR)) {
            parser.setDefaultOperator(Operator.AND);
        }
        this.q = parser.parse(query.getString("q"));

        // Filter out items from other views.
        final TermsFilter filter = new TermsFilter();
        filter.addTerm(new Term(Config.VIEW, this.viewname));

        this.filter = FilterCache.get(this.viewname, filter);

        // Parse sort order.
        final String sort = query.optString("sort", null);
        if (sort == null) {
            this.sort = null;
        } else {
            final String[] split = sort.split(",");
            final SortField[] sort_fields = new SortField[split.length];
            for (int i = 0; i < split.length; i++) {
                switch (split[i].charAt(0)) {
                case '/':
                    sort_fields[i] = new SortField(split[i].substring(1));
                    break;
                case '\\':
                    sort_fields[i] = new SortField(split[i].substring(1), true);
                    break;
                default:
                    sort_fields[i] = new SortField(split[i]);
                    break;
                }
            }

            if (sort_fields.length == 1) {
                // Let Lucene add doc as secondary sort order.
                this.sort = new Sort(sort_fields[0].getField(), sort_fields[0].getReverse());
            } else {
                this.sort = new Sort(sort_fields);
            }
        }
    }

    public String execute(final IndexSearcher searcher) throws IOException {
        // Return "304 - Not Modified" if etag matches.
        final String etag = getETag(searcher);
        if (!debug && etag.equals(this.ifNoneMatch)) {
            return "{\"code\":304}";
        }

        final JSONObject json = new JSONObject();
        json.put("q", q.toString());
        json.put("etag", etag);
        json.put("view_sig", viewsig);

        if (rewrite_query) {
            final Query rewritten_q = q.rewrite(searcher.getIndexReader());
            json.put("rewritten_q", rewritten_q.toString());

            final JSONObject freqs = new JSONObject();

            final Set terms = new HashSet();
            rewritten_q.extractTerms(terms);
            Iterator i = terms.iterator();
            while (i.hasNext()) {
                final Object term = i.next();
                final int freq = searcher.docFreq((Term) term);
                freqs.put(term, new Integer(freq));
            }
            json.put("freqs", freqs);
        } else {
            // Perform search.
            final TopDocs td;
            final StopWatch stopWatch = new StopWatch();

            if (sort == null) {
                td = searcher.search(q, filter, skip + limit);
            } else {
                td = searcher.search(q, filter, skip + limit, sort);
            }
            stopWatch.lap("search");
            // Fetch matches (if any).
            final int max = Math.max(0, Math.min(td.totalHits - skip, limit));

            final JSONArray rows = new JSONArray();
            final String[] fetch_ids = new String[max];
            for (int i = skip; i < skip + max; i++) {
                final Document doc = searcher.doc(td.scoreDocs[i].doc);
                final JSONObject row = new JSONObject();
                final JSONObject fields = new JSONObject();

                // Include stored fields.
                Iterator it = doc.getFields().iterator();
                while (it.hasNext()) {
                    Object f = it.next();
                    Field fld = (Field) f;

                    if (!fld.isStored())
                        continue;
                    String name = fld.name();
                    String value = fld.stringValue();
                    if (value != null) {
                        if (Config.ID.equals(name)) {
                            row.put("id", value);
                        } else {
                            if (!fields.has(name)) {
                                fields.put(name, value);
                            } else {
                                final Object obj = fields.get(name);
                                if (obj instanceof String) {
                                    final JSONArray arr = new JSONArray();
                                    arr.add((String) obj);
                                    arr.add(value);
                                    fields.put(name, arr);
                                } else {
                                    assert obj instanceof JSONArray;
                                    ((JSONArray) obj).add(value);
                                }
                            }
                        }
                    }
                }

                row.put("score", new Float(td.scoreDocs[i].score));
                // Include sort order (if any).
                if (td instanceof TopFieldDocs) {
                    final FieldDoc fd = (FieldDoc) ((TopFieldDocs) td).scoreDocs[i];
                    row.put("sort_order", fd.fields);
                }
                // Fetch document (if requested).
                if (include_docs) {
                    fetch_ids[i - skip] = doc.get(Config.ID);
                }
                if (fields.size() > 0) {
                    row.put("fields", fields);
                }
                rows.add(row);
            }
            // Fetch documents (if requested).
            if (include_docs) {
                final JSONArray fetched_docs = DB.getDocs(dbname, fetch_ids).getJSONArray("rows");
                for (int i = 0; i < max; i++) {
                    rows.getJSONObject(i).put("doc", fetched_docs.getJSONObject(i).getJSONObject("doc"));
                }
            }
            stopWatch.lap("fetch");

            json.put("skip", new Integer(skip));
            json.put("limit", new Integer(limit));
            json.put("total_rows", new Integer(td.totalHits));
            json.put("search_duration", new Long(stopWatch.getElapsed("search")));
            json.put("fetch_duration", new Long(stopWatch.getElapsed("fetch")));
            // Include sort info (if requested).
            if (td instanceof TopFieldDocs) {
                json.put("sort_order", toString(((TopFieldDocs) td).fields));
            }
            json.put("rows", rows);
        }

        final JSONObject result = new JSONObject();
        result.put("code", new Integer(200));

        final JSONObject headers = new JSONObject();
        headers.put("Content-Type", contentType);
        // Allow short-term caching.
        headers.put("Cache-Control", "max-age=" + Config.COMMIT_MIN / 1000);
        // Results can't change unless the IndexReader does.
        headers.put("ETag", etag);

        if (debug) {
            headers.put("Content-Type", "text/plain;charset=utf-8");
            result.put("body", escape(json.toString(2)));
        } else if (callback != null)
            result.put("body", callback + "(" + json + ")");
        else
            result.put("json", json);

        // Include headers.
        result.put("headers", headers);

        return result.toString();
    }

    private String escape(final String str) {
        final StringBuffer builder = new StringBuffer(str.length() + 10);
        builder.append(DOUBLE_QUOTE);
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == DOUBLE_QUOTE)
                builder.append("\"");
            else
                builder.append(c);
        }
        builder.append(DOUBLE_QUOTE);
        return builder.toString();
    }

    private String getETag(final IndexSearcher searcher) {
        return Long.toHexString(searcher.getIndexReader().getVersion());
    }

    private String toString(final SortField[] sortFields) {
        final JSONArray result = new JSONArray();
        for (int i = 0; i < sortFields.length; i++) {
            final SortField field = sortFields[i];
            final JSONObject col = new JSONObject();
            col.element("field", field.getField());
            col.element("reverse", field.getReverse());

            final String type;
            switch (field.getType()) {
            case SortField.DOC:
                type = "doc";
                break;
            case SortField.SCORE:
                type = "score";
                break;
            case SortField.INT:
                type = "int";
                break;
            case SortField.LONG:
                type = "long";
                break;
            case SortField.BYTE:
                type = "byte";
                break;
            case SortField.CUSTOM:
                type = "custom";
                break;
            case SortField.DOUBLE:
                type = "double";
                break;
            case SortField.FLOAT:
                type = "float";
                break;
            case SortField.SHORT:
                type = "short";
                break;
            case SortField.STRING:
                type = "string";
                break;
            default:
                type = "unknown";
                break;
            }
            col.element("type", type);
            result.add(col);
        }
        return result.toString();
    }

}
