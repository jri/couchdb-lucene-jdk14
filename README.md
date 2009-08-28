<h1>About this fork</h1>

This is a JDK 1.4 backport of Robert Newson's "couchdb-lucene" version 0.4.

Why JDK 1.4? Because my system is a 5-year old Mac running Mac OS X 10.3.9 ("Panther") and I'm pretty happy with it.

Differences to Robert Newson's release:

<ul>
<li>The tests are not ported and can't be run.
<li>No Maven build file yet. (Robert Newson's one is for Maven 2 and requires Java 1.5)
<li>The dependencies are not included and must be obtained separately.
</ul>

List of Dependencies:

<pre>
lucene-core-2.4.1.jar
lucene-analyzers-2.4.1.jar
lucene-queries-2.4.1.jar
json-lib-2.3-jdk13.jar
js-14.jar (rhino1_7R2)
commons-httpclient-3.1.jar
commons-io-1.4.jar
commons-beanutils-core-1.8.0.jar
commons-collections-3.2.1.jar
commons-codec-1.4.jar
commons-lang-2.4.jar
commons-logging-1.1.1.jar
log4j-1.2.15.jar
ezmorph-1.0.6.jar
tika-core-0.4-jdk14.jar (for indexing files)
tika-parsers-0.4-jdk14.jar (for indexing files)
pdfbox-0.7.3.jar (for parsing PDF files)
fontbox-0.1.0.jar (for parsing PDF files)
poi-3.2.jar (for parsing Microsoft files, e.g. Word)
poi-scratchpad-3.2.jar (for parsing Microsoft files, e.g. Word)
nekohtml-1.9.9.jar (for parsing HTML files)
</pre>

Note: tika-core-0.4-jdk14.jar and tika-parsers-0.4-jdk14.jar are also based on my JDK 1.4 backports. You can get the binaries from the Downloads section.

<h1>Issue Tracking</h1>

Issue tracking at <a href="http://github.com/jri/couchdb-lucene-jdk14/issues">github</a>.

<h1>System Requirements</h1>

JDK 1.4 or higher is recommended.

<h1>Build couchdb-lucene-jdk14</h1>

You can build couchdb-lucene-0.4-jdk14.jar manually by performing these steps:

<ol>
<li>Checkout repository
<li>Compiling the sources
    <ol>
    <li>go to couchdb-lucene-jdk14/src/main/java/com/github/rnewson/couchdb/lucene and compile with "javac -source 1.4"
    <li>go to couchdb-lucene-jdk14/src/main/java/org/apache/nutch/analysis/lang and compile the sources.
    </ol>
<li>Building the jar
    <ol>
    <li>cd couchdb-lucene-jdk14/src/main/java
    <li>jar cfm couchdb-lucene-0.4-jdk14.jar MANIFEST.MF com/github/rnewson/couchdb/lucene/*.class org/apache/nutch/analysis/lang/*.class
    <li>cd ../resources/
    <li>jar uf ../java/couchdb-lucene-0.4-jdk14.jar *
    </ol>
</ol>

Alternatively you can get the binary from the Downloads section.

<h1>Configure CouchDB</h1>

<pre>
[couchdb]
os_process_timeout=60000 ; increase the timeout from 5 seconds.

[external]
fti=/usr/bin/java -server -jar /path/to/couchdb-lucene-0.4-jdk14.jar -search

[update_notification]
indexer=/usr/bin/java -server -jar /path/to/couchdb-lucene-0.4-jdk14.jar -index

[httpd_db_handlers]
_fti = {couch_httpd_external, handle_external_req, <<"fti">>}
</pre>


The remainder is from Robert Newson's original README.
vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv


<h1>Indexing Strategy</h1>

<h2>Document Indexing</h2>

You must supply a index function in order to enable couchdb-lucene as, by default, nothing will be indexed. To suppress a document from the index, return null. It's more typical to return a single Document object which contains everything you'd like to query and retrieve. You may also return an array of Document objects if you wish.

You may add any number of index views in any number of design documents. All searches will be constrained to documents emitted by the index functions.

Here's an complete example of a design document with couchdb-lucene features:

<pre>
{
    "_id":"_design/a_design_document_with_any_name_you_like",
    "fulltext": {
        "by_subject": {
            "defaults": { "store":"yes" },
            "index":"function(doc) { var ret=new Document(); ret.add(doc.subject); return ret }"
        },
        "by_content": {
            "defaults": { "store":"no" },
            "index":"function(doc) { var ret=new Document(); ret.add(doc.content); return ret }"
        }
    }
}
</pre>

Here are some example URL's for the given design document;

<pre>
http://localhost:5984/database/_fti/lucene/by_subject?q=hello
http://localhost:5984/database/_fti/lucene/by_content?q=hello
</pre>

A fulltext object contains multiple index view declarations. An index view consists of;

<dl>
<dt>analyzer</dt><dd>(optional) The analyzer to use</dd>
<dt>defaults</dt><dd>(optional) The default for numerous indexing options can be overridden here. A full list of options follows.</dd>
<dt>index</dt><dd>The indexing function itself, documented below.</dd>
</dl>

<h3>The Defaults Object</h3>

The following indexing options can be defaulted;

<table>
  <tr>
    <th>name</th>
    <th>description</th>
    <th>available options</th>
    <th>default</th>
  </tr>
  <tr>
    <th>field</th>
    <td>the field name to index under</td>
    <td>user-defined</td>
    <td>default</td>
  </tr>	
  <tr>
    <th>store</th>
    <td>whether the data is stored. The value will be returned in the search result.</td>
    <td>yes, no</td>
    <td>no</td>
  </tr>	
  <tr>
    <th>index</th>
    <td>whether (and how) the data is indexed</td>
    <td>analyzed, analyzed_no_norms, no, not_analyzed, not_analyzed_no_norms</td>
    <td>analyzed</td>
  </tr>	
</table>

<h3>The Analyzer Option</h3>

Lucene has numerous ways of converting free-form text into tokens, these classes are called Analyzer's. By default, the StandardAnalyzer is used which lower-cases all text, drops common English words ("the", "and", and so on), among other things. This processing might not always suit you, so you can choose from several others by setting the "analyzer" field to one of the following values;

<ul>
<li>brazilian</li>
<li>chinese</li>
<li>cjk</li>
<li>czech</li>
<li>dutch</li>
<li>english</li>
<li>french</li>
<li>german</li>
<li>keyword</li>
<li>porter</li>
<li>russian</li>
<li>simple</li>
<li>standard</li>
<li>thai</li>
</ul>

Note: You must also supply analyzer=<analyzer_name> as a query parameter to ensure that queries are processed correctly.

<h3>The Document class</h3>

You may construct a new Document instance with;

<pre>
var doc = new Document();
</pre>

Data may be added to this document with the add method which takes an optional second object argument that can override any of the above default values.

The data is usually interpreted as a String but couchdb-lucene provides special handling if a Javascript Date object is passed. Specifically, the date is indexed as a numeric value, which allows correct sorting, and stored (if requested) in ISO 8601 format (with a timezone marker).

<pre>
// Add with all the defaults.
doc.add("value");

// Add a subject field.
doc.add("this is the subject line.", {"field":"subject"});

// Add but ensure it's stored.
doc.add("value", {"store":"yes"});

// Add but don't analyze.
doc.add("don't analyze me", {"index":"not_analyzed"});

// Extract text from the named attachment and index it (but not store it).
doc.attachment("attachment name", {"field":"attachments"});
</pre>

<h3>Example Transforms</h3>

<h4>Index Everything</h4>

<pre>
function(doc) {
    var ret = new Document();

    function idx(obj) {
	for (var key in obj) {
	    switch (typeof obj[key]) {
	    case 'object':
		idx(obj[key]);
		break;
	    case 'function':
		break;
	    default:
		ret.add(obj[key]);
		break;
	    }
	}
    };

    idx(doc);

    if (doc._attachments) {
	for (var i in doc._attachments) {
	    ret.attachment("attachment", i);
	}
    }
    
    return ret;
}
</pre>

<h4>Index Nothing</h4>

<pre>
function(doc) {
  return null;
}
</pre>

<h4>Index Select Fields</h4>

<pre>
function(doc) {
  var result = new Document();
  result.add(doc.subject, {"field":"subject", "store":"yes"});
  result.add(doc.content, {"field":"subject"});
  result.add({"field":"indexed_at"});
  return result;
}
</pre>

<h4>Index Attachments</h4>

<pre>
function(doc) {
  var result = new Document();
  for(var a in doc._attachments) {
    result.add_attachment(a, {"field":"attachment"});
  }
  return result;
}
</pre>

<h4>A More Complex Example</h4>

<pre>
function(doc) {
    var mk = function(name, value, group) {
        var ret = new Document();
        ret.add(value, {"field": group, "store":"yes"});
        ret.add(group, {"field":"group", "store":"yes"});
        return ret;
    };
    var ret = [];
    if(doc.type != "reference") return null;
    for(var g in doc.groups) {
        ret.add(mk("library", doc.groups[g].library, g));
        ret.add(mk("method", doc.groups[g].method, g));
        ret.add(mk("target", doc.groups[g].target, g));
    }
    return ret;
}
</pre>

<h2>Attachment Indexing</h2>

Couchdb-lucene uses <a href="http://lucene.apache.org/tika/">Apache Tika</a> to index attachments of the following types, assuming the correct content_type is set in couchdb;

<h3>Supported Formats</h3>

<ul>
<li>Excel spreadsheets (application/vnd.ms-excel)
<li>HTML (text/html)
<li>Images (image/*)
<li>Java class files
<li>Java jar archives
<li>MP3 (audio/mp3)
<li>OpenDocument (application/vnd.oasis.opendocument.*)
<li>Outlook (application/vnd.ms-outlook)
<li>PDF (application/pdf)
<li>Plain text (text/plain)
<li>Powerpoint presentations (application/vnd.ms-powerpoint)
<li>RTF (application/rtf)
<li>Visio (application/vnd.visio)
<li>Word documents (application/msword)
<li>XML (application/xml)
</ul>

<h1>Searching with couchdb-lucene</h1>

You can perform all types of queries using Lucene's default <a href="http://lucene.apache.org/java/2_4_0/queryparsersyntax.html">query syntax</a>. The _body field is searched by default which will include the extracted text from all attachments. The following parameters can be passed for more sophisticated searches;

<dl>
<dt>analyzer</dt><dd>The analyzer used to convert the query string into a query object.
<dt>callback</dt><dd>Specify a JSONP callback wrapper. The full JSON result will be prepended with this parameter and also placed with parentheses."
<dt>debug</dt><dd>Setting this to true disables response caching (the query is executed every time) and indents the JSON response for readability.</dd>
<dt>force_json<dt><dd>Usually couchdb-lucene determines the Content-Type of its response based on the presence of the Accept header. If Accept contains "application/json", you get "application/json" in the response, otherwise you get "text/plain;charset=utf8". Some tools, like JSONView for FireFox, do not send the Accept header but do render "application/json" responses if received. Setting force_json=true forces all response to "application/json" regardless of the Accept header.</dd>
<dt>include_docs</dt><dd>whether to include the source docs</dd>
<dt>limit</dt><dd>the maximum number of results to return</dd>
<dt>q</dt><dd>the query to run (e.g, subject:hello). If not specified, the default field is searched.</dd>
<dt>rewrite</dt><dd>(EXPERT) if true, returns a json response with a rewritten query and term frequencies. This allows correct distributed scoring when combining the results from multiple nodes.</dd>
<dt>skip</dt><dd>the number of results to skip</dd>
<dt>sort</dt><dd>the comma-separated fields to sort on. Prefix with / for ascending order and \ for descending order (ascending is the default if not specified).</dd>
<dt>stale=ok</dt><dd>If you set the <i>stale</i> option to <i>ok</i>, couchdb-lucene may not perform any refreshing on the index. Searches may be faster as Lucene caches important data (especially for sorting). A query without stale=ok will use the latest data committed to the index.</dd>
</dl>

<i>All parameters except 'q' are optional.</i>

<h2>Special Fields</h2>

<dl>
<dt>_db</dt><dd>The source database of the document.</dd>
<dt>_id</dt><dd>The _id of the document.</dd>
</dl>

<h2>Dublin Core</h2>

All Dublin Core attributes are indexed and stored if detected in the attachment. Descriptions of the fields come from the Tika javadocs.

<dl>
<dt>_dc.contributor</dt><dd> An entity responsible for making contributions to the content of the resource.</dd>
<dt>_dc.coverage</dt><dd>The extent or scope of the content of the resource.</dd>
<dt>_dc.creator</dt><dd>An entity primarily responsible for making the content of the resource.</dd>
<dt>_dc.date</dt><dd>A date associated with an event in the life cycle of the resource.</dd>
<dt>_dc.description</dt><dd>An account of the content of the resource.</dd>
<dt>_dc.format</dt><dd>Typically, Format may include the media-type or dimensions of the resource.</dd>
<dt>_dc.identifier</dt><dd>Recommended best practice is to identify the resource by means of a string or number conforming to a formal identification system.</dd>
<dt>_dc.language</dt><dd>A language of the intellectual content of the resource.</dd>
<dt>_dc.modified</dt><dd>Date on which the resource was changed.</dd>
<dt>_dc.publisher</dt><dd>An entity responsible for making the resource available.</dd>
<dt>_dc.relation</dt><dd>A reference to a related resource.</dd>
<dt>_dc.rights</dt><dd>Information about rights held in and over the resource.</dd>
<dt>_dc.source</dt><dd>A reference to a resource from which the present resource is derived.</dd>
<dt>_dc.subject</dt><dd>The topic of the content of the resource.</dd>
<dt>_dc.title</dt><dd>A name given to the resource.</dd>
<dt>_dc.type</dt><dd>The nature or genre of the content of the resource.</dd>
</dl>

<h2>Examples</h2>

<pre>
http://localhost:5984/dbname/_fti/design_doc/view_name?q=field_name:value
http://localhost:5984/dbname/_fti/design_doc/view_name?q=field_name:value&sort=other_field
http://localhost:5984/dbname/_fti/design_doc/view_name?debug=true&sort=billing_size&q=body:document AND customer:[A TO C]
</pre>

<h2>Search Results Format</h2>

The search result contains a number of fields at the top level, in addition to your search results.

<dl>
<dt>etag</dt><dd>An opaque token that reflects the current version of the index. This value is also returned in an ETag header to facilitate HTTP caching.</dd>
<dt>fetch_duration</dt><dd>The number of milliseconds spent retrieving the documents.</dd>
<dt>limit</dt><dd>The maximum number of results that can appear.</dd>
<dt>q</dt><dd>The query that was executed.</dd>
<dt>rows</dt><dd>The search results array, described below.</dd>
<dt>search_duration</dt><dd>The number of milliseconds spent performing the search.</dd>
<dt>skip</dt><dd>The number of initial matches that was skipped.</dd>
<dt>total_rows</dt><dd>The total number of matches for this query.</dd>
</dl>

<h2>The search results array</h2>

The search results arrays consists of zero, one or more objects with the following fields;

<dl>
<dt>doc</dt><dd>The original document from couch, if requested with include_docs=true</dd>
<dt>fields</dt><dd>All the fields that were stored with this match</dd>
<dt>id</dt><dd>The unique identifier for this match.</dd>
<dt>score</dt><dd>The normalized score (0.0-1.0, inclusive) for this match</dd>
</dl>

Here's an example of a JSON response without sorting;

<pre>
{
  "q": "+content:enron",
  "skip": 0,
  "limit": 2,
  "total_rows": 176852,
  "search_duration": 518,
  "fetch_duration": 4,
  "rows":   [
        {
      "id": "hain-m-all_documents-257.",
      "score": 1.601625680923462
    },
        {
      "id": "hain-m-notes_inbox-257.",
      "score": 1.601625680923462
    }
  ]
}
</pre>

And the same with sorting;

<pre>
{
  "q": "+content:enron",
  "skip": 0,
  "limit": 3,
  "total_rows": 176852,
  "search_duration": 660,
  "fetch_duration": 4,
  "sort_order":   [
        {
      "field": "source",
      "reverse": false,
      "type": "string"
    },
        {
      "reverse": false,
      "type": "doc"
    }
  ],
  "rows":   [
        {
      "id": "shankman-j-inbox-105.",
      "score": 0.6131107211112976,
      "sort_order":       [
        "enron",
        6
      ]
    },
        {
      "id": "shankman-j-inbox-8.",
      "score": 0.7492915391921997,
      "sort_order":       [
        "enron",
        7
      ]
    },
        {
      "id": "shankman-j-inbox-30.",
      "score": 0.507369875907898,
      "sort_order":       [
        "enron",
        8
      ]
    }
  ]
}
</pre>

<h3>Content-Type of response</h3>

The Content-Type of the response is negotiated via the Accept request header like CouchDB itself. If the Accept header includes "application/json" then that is also the Content-Type of the response. If not, "text/plain;charset=utf-8" is used. 

<h1>Fetching information about the index</h1>

Calling couchdb-lucene without arguments returns a JSON object with information about the <i>whole</i> index.

<pre>
http://127.0.0.1:5984/enron/_fti
</pre>

returns;

<pre>
{"doc_count":517350,"doc_del_count":1,"disk_size":318543045}
</pre>

<h1>Working With The Source</h1>

To develop "live", type "mvn dependency:unpack-dependencies" and change the external line to something like this;

<pre>
fti=/usr/bin/java -server -cp /path/to/couchdb-lucene/target/classes:\
/path/to/couchdb-lucene/target/dependency com.github.rnewson.couchdb.lucene.Main
</pre>

You will need to restart CouchDB if you change couchdb-lucene source code but this is very fast.

<h1>Configuration</h1>

couchdb-lucene respects several system properties;

<dl>
<dt>couchdb.log.dir</dt><dd>specify the directory of the log file (which is called couchdb-lucene.log), defaults to the platform-specific temp directory.</dd>
<dt>couchdb.lucene.dir</dt><dd>specify the path to the lucene indexes (the default is to make a directory called 'lucene' relative to couchdb's current working directory.</dd>
<dt>couchdb.lucene.operator<dt><dd>specify the default boolean operator for queries. If not specified, the default is "OR". You can specify either "OR" or "AND".</dd>
<dt>couchdb.url</dt><dd>the url to contact CouchDB with (default is "http://localhost:5984")</dd>
</dl>

You can override these properties like this;

<pre>
fti=/usr/bin/java -Dcouchdb.lucene.dir=/tmp \
-cp /home/rnewson/Source/couchdb-lucene/target/classes:\
/home/rnewson/Source/couchdb-lucene/target/dependency\
com.github.rnewson.couchdb.lucene.Main
</pre>

<h2>Basic Authentication</h2>

If you put couchdb behind an authenticating proxy you can still configure couchdb-lucene to pull from it by specifying additional system properties. Currently only Basic authentication is supported.

<dl>
<dt>couchdb.password</dt><dd>the password to authenticate with.</dd>
<dt>couchdb.user</dt><dd>the user to authenticate as.</dd>
</dl>

<h2>IPv6</h2>

The default for couchdb.url is problematic on an IPv6 system. Specify -Dcouchdb.url=http://[::1]:5984 to resolve it.
