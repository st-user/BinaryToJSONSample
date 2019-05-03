package com.example;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
* このデモコードでは、以下ような状況をシミュレートしています。
* 
* 「read」の例（read = JSON to javaオブジェクトの変換。Deserializeとも呼ばれる)
* 例1: 「 InputStream inputStream = request.getInputStream(); 」のJSONをjavaのオブジェクトに変換
* (「request」はHttpServletRequest。リクエストボディは任意のエンコーディングでエンコードされている)
* 例2: 「 File file = new File(....); 」のJSONをjavaのオブジェクトに変換
* (「file」のデータは任意のエンコーディングでエンコードされている)
* 
* 「write」の例（write = javaオブジェクト to JSONの変換。Serializeとも呼ばれる)
* 例1: 「 OutputStream outputStream = response.getOutputStream(); 」にJSONを書き出す
* (「response」はHttpServletResponse。任意のエンコーディングでレスポンスボディをエンコードする必要がある)
* 例2: 「 File file = new File(....); 」にJSONを書き出す
* (「file」のデータは任意のエンコーディングでエンコードする必要がある)
* 
 */
public class BinaryToJSONSampleTest {

	ObjectMapper mapper = new ObjectMapper();

	String json = "{\"message\":\"あいうえお\"}"; // 「read」するJSON
	String readExpected = new Bean("あいうえお").toString();// 「read」の期待値
	Bean bean = new Bean("かきくけこ");// 「write」するjavaのオブジェクト
	String writeExpected = "{\"message\":\"かきくけこ\"}";// 「write」の期待値
	
	static class Bean {
		
		private String message;

		public Bean() {
		}

		public Bean(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			return "Bean [message=" + message + "]";
		}
	}

	/*
	 * (a) UTF-8, UTF-16, UTF-32を「read」する場合は、エンコーディングの指定は不要
	 * 
	 * String#getBytesを使用し、任意のエンコーディングのバイトデータが与えられた状況をシミュレートしています
	 * 確認に使用しているメソッド :
	 *  ObjectMapper#readValue(byte[], Class<T>)<T> : T 
	 */
	@Test
	public void readValueFromUTF8or16or32ByteArrayShouldDetectEncodingSuccessfully() throws Exception {

		Bean beanFromUTF32BE = mapper.readValue(json.getBytes("UTF-32BE"), Bean.class);
		Bean beanFromUTF16BE = mapper.readValue(json.getBytes("UTF-16BE"), Bean.class);
		Bean beanFromUTF32LE = mapper.readValue(json.getBytes("UTF-32LE"), Bean.class);
		Bean beanFromUTF16LE = mapper.readValue(json.getBytes("UTF-16LE"), Bean.class);
		Bean beanFromUTF8 = mapper.readValue(json.getBytes("UTF-8"), Bean.class);
		
		assertThat(readExpected, equalTo(beanFromUTF32BE.toString()));
		assertThat(readExpected, equalTo(beanFromUTF16BE.toString()));
		assertThat(readExpected, equalTo(beanFromUTF32LE.toString()));
		assertThat(readExpected, equalTo(beanFromUTF16LE.toString()));
		assertThat(readExpected, equalTo(beanFromUTF8.toString()));
		
		// !! 以下を実行するとExceptionが発生します。
		// mapper.readValue(json.getBytes("Shift_JIS"), Bean.class);
		// -> 「com.fasterxml.jackson.databind.JsonMappingException: Invalid UTF-8 start byte 0x82」
		
	}

	/*
	 * (a)
	 * 上記の「 ObjectMapper#readValue(byte[], Class<T>)<T> : T 」以外の
	 * 「(a)」パターンとして利用できるメソッド
	 */
	@Test
	public void readMethodsFromAnyUTF8BinaryDataShouldDetectEncodingSuccessfully() throws Exception {
		
		// !!! 現実のアプリケーションのイメージ
		// 例えば、「InputStream」が HttpServletRequest#getInputStreamによって
		// 与えられた状況をイメージしてください。
		
		File file = new File("sample_UTF-8.json");
		// 第一引数 : URL
		Bean beanFromURL = mapper.readValue(file.toURI().toURL(), Bean.class);
		assertThat(readExpected, equalTo(beanFromURL.toString()));
		
		// 第一引数 : File
		Bean beanFromFile = mapper.readValue(file, Bean.class);
		assertThat(readExpected, equalTo(beanFromFile.toString()));
		
		// 第一引数 : InputStream
		InputStream inputStream = new ByteArrayInputStream(json.getBytes("UTF-8"));
		Bean beanFromInputStream = mapper.readValue(inputStream, Bean.class);
		assertThat(readExpected, equalTo(beanFromInputStream.toString()));
		
		// 第一引数 : DataInput
		DataInput dataInput = new DataInputStream(new FileInputStream(file));
		Bean beanFromDataInput = mapper.readValue(dataInput, Bean.class);
		assertThat(readExpected, equalTo(beanFromDataInput.toString()));
		
		// 第一引数: JsonParser
		// 「createParser」に渡せる引数の種類は、「readValue」の第一引数と、ほぼ同等です
		JsonParser jsonParser = mapper.getFactory().createParser(json.getBytes("UTF-8"));
		Bean beanFromJsonParser = mapper.readValue(jsonParser, Bean.class);
		assertThat(readExpected, equalTo(beanFromJsonParser.toString()));

		// 「readTree」の場合も、「readValue」と同じような引数を渡せます。
		// 以下は、byte[]を渡す例。他にも、URL, File, InputStream, JsonParserを渡すメソッドがあります。
		JsonNode jsonNode = mapper.readTree(json.getBytes("UTF-8"));
		assertThat("あいうえお",  equalTo(jsonNode.get("message").asText()));
		
	}

	/*
	 * (b) UTF-8, UTF-16, UTF-32以外を「read」する場合は、
	 *  エンコーディングを明示したReaderを生成し、Readerを引数に渡すタイプのメソッドを使用する
	 * 
	 * String#getBytesを使用し、任意のエンコーディングのバイトデータが与えられた状況をシミュレートしています
	 * 確認に使用しているメソッド :
	 *  ObjectMapper#readValue(Reader, Class<T>)<T> : T 
	 */
	@Test
	public void readValueFromReaderShouldReadBytesOfSpecifiedEncoding() throws Exception {

		// !!! 現実のアプリケーションのイメージ
		// 例えば、「InputStream」が HttpServletRequest#getInputStreamによって
		// 与えられた状況をイメージしてください。

		InputStream inputStream = new ByteArrayInputStream(json.getBytes("Shift_JIS"));
		Reader reader = new InputStreamReader(inputStream, "Shift_JIS");
		Bean beanFromReader = mapper.readValue(reader, Bean.class);
		assertThat(readExpected, equalTo(beanFromReader.toString()));
	}
	
	/*
	 * (b)
	 * 上記の「 ObjectMapper#readValue(Reader, Class<T>)<T> : T 」以外の
	 * 「(b)」パターンとして利用できるメソッド
	 */
	@Test
	public void readMethodsFromReaderShouldReadBytesOfSpecifiedEncoding() throws Exception {

		// 第一引数: JsonParser
		// 「createParser」にReaderを渡す
		Reader reader = new InputStreamReader(new ByteArrayInputStream(json.getBytes("Shift_JIS")), "Shift_JIS");
		JsonParser jsonParser = mapper.getFactory().createParser(reader);
		Bean beanFromJsonParser = mapper.readValue(jsonParser, Bean.class);
		assertThat(readExpected, equalTo(beanFromJsonParser.toString()));

		// 「readTree」の場合も、Readerを渡せます
		Reader readerToTree = new InputStreamReader(new ByteArrayInputStream(json.getBytes("Shift_JIS")), "Shift_JIS");
		JsonNode jsonNode = mapper.readTree(readerToTree);
		assertThat("あいうえお", equalTo(jsonNode.get("message").asText()));
	}
	
	/*
	 * (c) UTF-8で「write」する場合は、エンコーディングの指定は不要
	 * 
	 * ByteArrayOutputStreamを使用し、OutputStreamにJSONを書き出す状況をシミュレートしています
	 * 確認に使用しているメソッド :
	 *  ObjectMapper#writeValue(OutputStream, Object) : void
	 */
	@Test
	public void writeValueNotSpecifyingEncodingShouldWriteUTF8Bytes() throws Exception {

		// !!! 現実のアプリケーションのイメージ
		// 例えば、「OutputStream」が HttpServletResponse#getOutputStreamによって
		// 与えられた状況をイメージしてください。
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		mapper.writeValue(outputStream, bean);
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(outputStream.toByteArray()));
	}
	
	/*
	 * (c)
	 * 上記の「 ObjectMapper#writeValue(OutputStream, Object) : void 」以外の
	 * 「(c)」パターンとして利用できるメソッド
	 */
	@Test
	public void writeMethodsNotSpecifyingEncodingShouldWriteUTF8Bytes() throws Exception {

		// 第一引数 : File
		File outFile = new File("out_sample.json");
		mapper.writeValue(outFile, bean);
		byte[] fileData = Files.readAllBytes(outFile.toPath());
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(fileData));
		
		// 第一引数 : DataOutput
		ByteArrayOutputStream dataOutputBytes = new ByteArrayOutputStream();
		DataOutput dataOutput = new DataOutputStream(dataOutputBytes);
		mapper.writeValue(dataOutput, bean);
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(dataOutputBytes.toByteArray()));
		
		// 第一引数: JsonGenerator
		// 「createGenerator」に渡せる引数の種類は、「writeValue」の第一引数と、ほぼ同等です
		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(jsonGeneratorOutputBytes);
		mapper.writeValue(jsonGenerator, bean);
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(jsonGeneratorOutputBytes.toByteArray()));

		// 「writeValueAsBytes」
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(mapper.writeValueAsBytes(bean)));
		
		// 「writeTree」の第一引数は、「JsonGenerator」のため、
		// ことエンコーディングに関しては「writeValue」において「JsonGenerator」を使用する時と同じ要領で使用します
		ByteArrayOutputStream jsonGeneratorOutputBytesForTree = new ByteArrayOutputStream();
		JsonGenerator jsonGeneratorForTree = mapper.getFactory().createGenerator(jsonGeneratorOutputBytesForTree);
		JsonNode tree = mapper.readTree(writeExpected);
		mapper.writeTree(jsonGeneratorForTree, tree);
		assertThat(writeExpected.getBytes("UTF-8"), equalTo(jsonGeneratorOutputBytesForTree.toByteArray()));
	}
	
	/*
	 * (c)補足
	 * 「writeValue」にて、JsonGeneratorを使用する場合に関する補足
	 *  「createGenerator」の引数に「JsonEncoding」を渡せる場合は、UTF-16, UTF-32も使用できます
	 * 
	 */
	@Test
	public void writeValueByJsonGeneratorCanAlsoSpecifyUTF16orUTF32Encoding() throws Exception {

		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(jsonGeneratorOutputBytes,
				JsonEncoding.UTF16_BE);
		mapper.writeValue(jsonGenerator, bean);

		assertThat(writeExpected.getBytes("UTF-16BE"), equalTo(jsonGeneratorOutputBytes.toByteArray()));
	}

	/*
	 * (d) UTF-8以外で「write」する場合は、
	 *  エンコーディングを明示したWriterを生成し、Writerを引数に渡すタイプのメソッドを使用する
	 * 
	 * ByteArrayOutputStreamを使用し、OutputStreamにJSONを書き出す状況をシミュレートしています
	 * 確認に使用しているメソッド :
	 *  ObjectMapper#writeValue(Writer, Object) : void
	 */
	@Test
	public void writeValueFromWriterShouldWriteBytesOfSpecifiedEncoding() throws Exception {

		// !!! 現実のアプリケーションのイメージ
		// 例えば、「OutputStream」が HttpServletResponse#getOutputStreamによって
		// 与えられた状況をイメージしてください。

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "Shift_JIS");
		mapper.writeValue(writer, bean);

		assertThat(writeExpected.getBytes("Shift_JIS"), equalTo(outputStream.toByteArray()));
	}

	/*
	 * (d)
	 * 上記の「 ObjectMapper#writeValue(Writer, Object) : void 」以外の
	 * 「(d)」パターンとして利用できるメソッド
	 */
	@Test
	public void writeMethodsFromWriterShouldWriteBytesOfSpecifiedEncoding() throws Exception {
		
		// 第一引数: JsonGenerator
		// 「createGenerator」に渡せる引数の種類は、「writeValue」の第一引数と、ほぼ同等です
		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(jsonGeneratorOutputBytes, "Shift_JIS");
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(writer);
		mapper.writeValue(jsonGenerator, bean);
		assertThat(writeExpected.getBytes("Shift_JIS"), equalTo(jsonGeneratorOutputBytes.toByteArray()));

		// 「writeTree」の第一引数は、「JsonGenerator」のため、
		// ことエンコーディングに関しては「writeValue」において「JsonGenerator」を使用する時と同じ要領で使用します
		ByteArrayOutputStream jsonGeneratorOutputBytesForTree = new ByteArrayOutputStream();
		OutputStreamWriter writerForTree = new OutputStreamWriter(jsonGeneratorOutputBytesForTree, "Shift_JIS");
		JsonGenerator jsonGeneratorForTree = mapper.getFactory().createGenerator(writerForTree);
		JsonNode tree = mapper.readTree(writeExpected);
		mapper.writeTree(jsonGeneratorForTree, tree);
		assertThat(writeExpected.getBytes("Shift_JIS"), equalTo(jsonGeneratorOutputBytesForTree.toByteArray()));
	}

}
