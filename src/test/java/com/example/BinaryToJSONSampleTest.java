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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * このデモコードでは、以下ような状況をシミュレートしています。
 * 
 * 「read」の例（read = JSON to javaオブジェクトの変換。Deserializeとも呼ばれる) 例1: 「 InputStream
 * inputStream = request.getInputStream(); 」のJSONをjavaのオブジェクトに変換
 * (「request」はHttpServletRequest。リクエストボディは任意のエンコーディングでエンコードされている) 例2: 「 File
 * file = new File(....); 」のJSONをjavaのオブジェクトに変換
 * (「file」のデータは任意のエンコーディングでエンコードされている)
 * 
 * 「write」の例（write = javaオブジェクト to JSONの変換。Serializeとも呼ばれる) 例1: 「 OutputStream
 * outputStream = response.getOutputStream(); 」にJSONを書き出す
 * (「response」はHttpServletResponse。任意のエンコーディングでレスポンスボディをエンコードする必要がある) 例2: 「 File
 * file = new File(....); 」にJSONを書き出す (「file」のデータは任意のエンコーディングでエンコードする必要がある)
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
	 *  確認に使用しているメソッド :
	 * ObjectMapper#readValue(byte[], Class<T>)<T> : T
	 */
	@Test
	public void readValueFromUTF8or16or32ByteArrayShouldDetectEncodingSuccessfully() throws Exception {

		Bean beanFromUTF32BE = mapper.readValue(json.getBytes("UTF-32BE"), Bean.class);
		Bean beanFromUTF16BE = mapper.readValue(json.getBytes("UTF-16BE"), Bean.class);
		Bean beanFromUTF32LE = mapper.readValue(json.getBytes("UTF-32LE"), Bean.class);
		Bean beanFromUTF16LE = mapper.readValue(json.getBytes("UTF-16LE"), Bean.class);
		Bean beanFromUTF8 = mapper.readValue(json.getBytes("UTF-8"), Bean.class);

		assertThat(beanFromUTF32BE.toString(), equalTo(readExpected));
		assertThat(beanFromUTF16BE.toString(), equalTo(readExpected));
		assertThat(beanFromUTF32LE.toString(), equalTo(readExpected));
		assertThat(beanFromUTF16LE.toString(), equalTo(readExpected));
		assertThat(beanFromUTF8.toString(), equalTo(readExpected));

		// !! 以下を実行するとExceptionが発生します。
		// mapper.readValue(json.getBytes("Shift_JIS"), Bean.class);
		// -> 「com.fasterxml.jackson.databind.JsonMappingException: Invalid UTF-8 start byte 0x82」

	}

	/*
	 * (a) 上記の「 ObjectMapper#readValue(byte[], Class<T>)<T> : T 」以外の
	 * 「(a)」パターンとして利用できるメソッド
	 */
	@Test
	public void readMethodsFromAnyUTF8BinaryDataShouldDetectEncodingSuccessfully() throws Exception {

		File file = new File("sample_UTF-8.json");
		// 第一引数 : URL
		Bean beanFromURL = mapper.readValue(file.toURI().toURL(), Bean.class);
		assertThat(beanFromURL.toString(), equalTo(readExpected));

		// 第一引数 : File
		Bean beanFromFile = mapper.readValue(file, Bean.class);
		assertThat(beanFromFile.toString(), equalTo(readExpected));

		// 第一引数 : InputStream
		InputStream inputStream = new FileInputStream(file);
		Bean beanFromInputStream = mapper.readValue(inputStream, Bean.class);
		assertThat(beanFromInputStream.toString(), equalTo(readExpected));

		// 第一引数: JsonParser
		// 「createParser」に渡せる引数の種類は、「readValue」の第一引数と似ています
		// 以下は、byte[]を渡す例です。
		// 他にも、File, URL, InputStream, Readerを渡すメソッド等があります
		JsonParser jsonParser = mapper.getFactory().createParser(json.getBytes("UTF-8"));
		Bean beanFromJsonParser = mapper.readValue(jsonParser, Bean.class);
		assertThat(beanFromJsonParser.toString(), equalTo(readExpected));

		// 「readTree」の場合も、「readValue」と同じような引数を渡せます。
		// 以下は、byte[]を渡す例です。
		JsonNode jsonNode = mapper.readTree(json.getBytes("UTF-8"));
		assertThat(jsonNode.get("message").asText(), equalTo("あいうえお"));

	}

	/*
	 * (b) UTF-8, UTF-16, UTF-32以外を「read」する場合は、
	 * エンコーディングを明示したReaderを生成し、Readerを引数に渡すタイプのメソッドを使用する
	 * 
	 * String#getBytesを使用し、任意のエンコーディングのバイトデータが与えられた状況をシミュレートしています 
	 * 確認に使用しているメソッド :
	 * ObjectMapper#readValue(Reader, Class<T>)<T> : T
	 */
	@Test
	public void readValueFromReaderShouldReadBytesOfSpecifiedEncoding() throws Exception {

		// 現実のアプリケーションの例：
		// 「InputStream」を HttpServletRequest#getInputStreamから取得する
		InputStream inputStream = new ByteArrayInputStream(json.getBytes("Shift_JIS"));
		Reader reader = new InputStreamReader(inputStream, "Shift_JIS");
		Bean beanFromReader = mapper.readValue(reader, Bean.class);
		assertThat(beanFromReader.toString(), equalTo(readExpected));
	}

	/*
	 * (b) 上記の「 ObjectMapper#readValue(Reader, Class<T>)<T> : T 」以外の
	 * 「(b)」パターンとして利用できるメソッド
	 */
	@Test
	public void readMethodsFromReaderShouldReadBytesOfSpecifiedEncoding() throws Exception {

		// 第一引数: JsonParser
		// 「createParser」にReaderを渡す
		Reader reader = new InputStreamReader(new ByteArrayInputStream(json.getBytes("Shift_JIS")), "Shift_JIS");
		JsonParser jsonParser = mapper.getFactory().createParser(reader);
		Bean beanFromJsonParser = mapper.readValue(jsonParser, Bean.class);
		assertThat(beanFromJsonParser.toString(), equalTo(readExpected));

		// 「readTree」でもReaderを渡す
		Reader readerToTree = new InputStreamReader(new ByteArrayInputStream(json.getBytes("Shift_JIS")), "Shift_JIS");
		JsonNode jsonNode = mapper.readTree(readerToTree);
		assertThat(jsonNode.get("message").asText(), equalTo("あいうえお"));
	}

	/*
	 * (c) UTF-8で「write」する場合は、エンコーディングの指定は不要
	 * 
	 * ByteArrayOutputStreamを使用し、OutputStreamにJSONを書き出す状況をシミュレートしています 
	 * 確認に使用しているメソッド
	 * : ObjectMapper#writeValue(OutputStream, Object) : void
	 */
	@Test
	public void writeValueNotSpecifyingEncodingShouldWriteUTF8Bytes() throws Exception {

		// 現実のアプリケーションの例：
		// 「OutputStream」を HttpServletResponse#getOutputStreamから取得する
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		mapper.writeValue(outputStream, bean);
		assertThat(outputStream.toByteArray(), equalTo(writeExpected.getBytes("UTF-8")));
	}

	/*
	 * (c) 上記の「 ObjectMapper#writeValue(OutputStream, Object) : void 」以外の
	 * 「(c)」パターンとして利用できるメソッド
	 */
	@Test
	public void writeMethodsNotSpecifyingEncodingShouldWriteUTF8Bytes() throws Exception {

		// 第一引数 : File
		File outFile = new File("out_sample.json");
		mapper.writeValue(outFile, bean);
		byte[] fileData = Files.readAllBytes(outFile.toPath());
		assertThat(fileData, equalTo(writeExpected.getBytes("UTF-8")));

		// 第一引数: JsonGenerator
		// 「createGenerator」に渡せる引数の種類は、「writeValue」の第一引数と似ています。
		// 以下はOutputStreamを渡す例です。
		// 他にもFileを渡すメソッド等があります。
		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(jsonGeneratorOutputBytes);
		mapper.writeValue(jsonGenerator, bean);
		assertThat(jsonGeneratorOutputBytes.toByteArray(), equalTo(writeExpected.getBytes("UTF-8")));

		// 「writeValueAsBytes」
		assertThat(mapper.writeValueAsBytes(bean), equalTo(writeExpected.getBytes("UTF-8")));

		// 「writeTree」の第一引数は、「JsonGenerator」のため、
		// ことエンコーディングに関しては「writeValue」において「JsonGenerator」を使用する時と同じ要領で使用します
		ByteArrayOutputStream jsonGeneratorOutputBytesForTree = new ByteArrayOutputStream();
		JsonGenerator jsonGeneratorForTree = mapper.getFactory().createGenerator(jsonGeneratorOutputBytesForTree);
		JsonNode tree = mapper.readTree(writeExpected);
		mapper.writeTree(jsonGeneratorForTree, tree);
		assertThat(jsonGeneratorOutputBytesForTree.toByteArray(), equalTo(writeExpected.getBytes("UTF-8")));
	}

	/*
	 * (c)補足 JsonGeneratorを使用する場合に関する補足
	 * 「createGenerator」の引数に「JsonEncoding」を渡せる場合は、UTF-16, UTF-32も使用できます
	 */
	@Test
	public void writeValueByJsonGeneratorCanAlsoSpecifyUTF16orUTF32Encoding() throws Exception {

		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(jsonGeneratorOutputBytes,
				JsonEncoding.UTF16_BE);
		mapper.writeValue(jsonGenerator, bean);

		assertThat(jsonGeneratorOutputBytes.toByteArray(), equalTo(writeExpected.getBytes("UTF-16BE")));
	}

	/*
	 * (d) UTF-8以外で「write」する場合は、 エンコーディングを明示したWriterを生成し、
	 * Writerを引数に渡すタイプのメソッドを使用する
	 * 
	 * ByteArrayOutputStreamを使用し、OutputStreamにJSONを書き出す状況をシミュレートしています 
	 * 確認に使用しているメソッド
	 * : ObjectMapper#writeValue(Writer, Object) : void
	 */
	@Test
	public void writeValueFromWriterShouldWriteBytesOfSpecifiedEncoding() throws Exception {

		// 現実のアプリケーションの例：
		// 「OutputStream」を HttpServletResponse#getOutputStreamから取得する
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "Shift_JIS");
		mapper.writeValue(writer, bean);

		assertThat(outputStream.toByteArray(), equalTo(writeExpected.getBytes("Shift_JIS")));
	}

	/*
	 * (d) 上記の「 ObjectMapper#writeValue(Writer, Object) : void 」以外の
	 * 「(d)」パターンとして利用できるメソッド
	 */
	@Test
	public void writeMethodsFromWriterShouldWriteBytesOfSpecifiedEncoding() throws Exception {

		// 第一引数: JsonGenerator
		// 「createGenerator」にWriterを渡す
		ByteArrayOutputStream jsonGeneratorOutputBytes = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(jsonGeneratorOutputBytes, "Shift_JIS");
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(writer);
		mapper.writeValue(jsonGenerator, bean);
		assertThat(jsonGeneratorOutputBytes.toByteArray(), equalTo(writeExpected.getBytes("Shift_JIS")));

		// 「writeTree」の第一引数は、「JsonGenerator」のため、
		// ことエンコーディングに関しては「writeValue」において「JsonGenerator」を使用する時と同じ要領で使用します
		ByteArrayOutputStream jsonGeneratorOutputBytesForTree = new ByteArrayOutputStream();
		OutputStreamWriter writerForTree = new OutputStreamWriter(jsonGeneratorOutputBytesForTree, "Shift_JIS");
		JsonGenerator jsonGeneratorForTree = mapper.getFactory().createGenerator(writerForTree);
		JsonNode tree = mapper.readTree(writeExpected);
		mapper.writeTree(jsonGeneratorForTree, tree);
		assertThat(jsonGeneratorOutputBytesForTree.toByteArray(), equalTo(writeExpected.getBytes("Shift_JIS")));
	}

	/*
	 * JsonGeneratorに関する補足 UTF-8以外のエンコーディングを使用し、
	 * 典型的なJsonGeneratorの使い方でJSONをwriteする例です。
	 */
	@Test
	public void jsonGeneratorShouldWriteJSONIncrementally() throws Exception {

		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(outputBytes, "Shift_JIS");
		JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(writer);
		jsonGenerator.writeStartObject();
		jsonGenerator.writeStringField("message", "あいうえお");
		jsonGenerator.writeEndObject();
		jsonGenerator.close();

		assertThat(outputBytes.toByteArray(), equalTo("{\"message\":\"あいうえお\"}".getBytes("Shift_JIS")));
	}

	/*
	 * JsonParserに関する補足 UTF-8以外のエンコーディングを使用し、 典型的なJsonParserの使い方でJSONをreadする例です
	 */
	@Test
	public void jsonParserShouldReadJSONIncrementally() throws Exception {

		Reader reader = new InputStreamReader(new ByteArrayInputStream(json.getBytes("Shift_JIS")), "Shift_JIS");
		JsonParser jsonParser = mapper.getFactory().createParser(reader);

		JsonToken t = null;
		String fieldName = null;
		String message = null;
		while ((t = jsonParser.nextToken()) != null) {
			if (t == JsonToken.FIELD_NAME) {
				fieldName = jsonParser.getCurrentName();
			}
			if (t == JsonToken.VALUE_STRING) {
				message = jsonParser.getText();
			}
		}
		assertThat(fieldName, equalTo("message"));
		assertThat(message, equalTo("あいうえお"));
	}

	/*
	 * DataInput / DataOutputの典型的な使用例 ・DataOutput :
	 * プリミティブ型または文字列を直接バイトデータとして書き出すために使用する ・DataInput :
	 * DataOutput等で書き出されたバイトデータを読み出すために使用する
	 */
	@Test
	public void personShouldBeSerializedByDataOutputAndDeserializedByDataInput() throws Exception {

		Person person = new Person();
		person.setAge(25);
		person.setName("𠮷太郎");
		person.setKanaCode('あ');
		File file = new File("sample_dataouput.dat");

		// DataOutputによるバイトデータの書き出し
		DataOutput dout = new DataOutputStream(new FileOutputStream(file));
		dout.writeInt(person.getAge());
		dout.writeUTF(person.getName());// 「modified UTF-8」で出力
		dout.writeChar(person.getKanaCode());

		// DataInputによるバイトデータの読み出し
		DataInput din = new DataInputStream(new FileInputStream(file));
		Person deserializedPerson = new Person();
		deserializedPerson.setAge(din.readInt());
		deserializedPerson.setName(din.readUTF());// 「modified UTF-8」として読み出す
		deserializedPerson.setKanaCode(din.readChar());

		assertThat(deserializedPerson.toString(), equalTo(person.toString()));
	}

	/*
	 * ObjectMapperで、DataInput / DataOutputを使用する例
	 */
	@Test
	public void person2ShouldBeSerializedByDataOutputAndDeserializedByDataInput() throws Exception {

		Person2 person = new Person2();
		person.setAge(25);
		PersonName personName = new PersonName();
		personName.setFirstname("𠮷太郎");
		personName.setLastname("山田");
		person.setPersonName(personName);
		person.setKanaCode('あ');
		File file = new File("sample_dataouput2.dat");

		// DataOutputによるバイトデータの書き出し
		DataOutput dout = new DataOutputStream(new FileOutputStream(file));
		dout.writeInt(person.getAge());
		// PersonNameはJSONで書き出す
		// 生成されたバイナリを確認するとUTF-8で出力されている
		mapper.writeValue(dout, person.getPersonName());
		dout.writeChar(person.getKanaCode());

		// DataInputによるバイトデータの読み出し
		DataInput din = new DataInputStream(new FileInputStream(file));
		Person2 deserializedPerson = new Person2();
		deserializedPerson.setAge(din.readInt());
		PersonName deserializedPersonName = mapper.readValue(din, PersonName.class);
		// !!!動作確認した限りでは、「writeValue」では「modified UTF-8」で出力されません。
		// そのため以下のように記述しても正しくDeserializeできませんでした
		// PersonName deserializedPersonName = mapper.readValue(din.readUTF(),
		// PersonName.class);
		deserializedPerson.setPersonName(deserializedPersonName);
		deserializedPerson.setKanaCode(din.readChar());

		assertThat(deserializedPerson.toString(), equalTo(person.toString()));
	}

	/*
	 * ObjectMapperで、DataInput / DataOutputを使用する場合の実験 「modified
	 * UTF-8」で書き出されたJSONであっても、 ObjectMapper#readValue(DataInput, Class<T>) <T>: T
	 * では正しくDeserializeできない !!! このテストは失敗します
	 */
	@Test
	public void modifiedUTF8StringShouldBeDeserializedByDataInput() throws Exception {

		File file = new File("sample_dataouput3.dat");

		// DataOutputによるバイトデータの書き出し
		DataOutput dout = new DataOutputStream(new FileOutputStream(file));
		dout.writeInt(55);
		dout.writeUTF("{\"firstname\":\"𠮷太郎\", \"lastname\":\"山田\"}");
		dout.writeChar('あ');

		// DataInputによるバイトデータの読み出し
		DataInput din = new DataInputStream(new FileInputStream(file));
		assertThat(55, equalTo(din.readInt()));
		PersonName deserializedPersonName = mapper.readValue(din, PersonName.class);
		// 以下のようにすると正しくDeserializeされる
		// PersonName deserializedPersonName = mapper.readValue(din.readUTF(),
		// PersonName.class);
		assertThat(deserializedPersonName.getFirstname(), equalTo("𠮷太郎"));
		assertThat(deserializedPersonName.getLastname(), equalTo("山田"));
		assertThat(din.readChar(), equalTo('あ'));
	}

	static class Person2 {

		private int age;
		private PersonName personName;
		private char kanaCode;

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public PersonName getPersonName() {
			return personName;
		}

		public void setPersonName(PersonName personName) {
			this.personName = personName;
		}

		public char getKanaCode() {
			return kanaCode;
		}

		public void setKanaCode(char kanaCode) {
			this.kanaCode = kanaCode;
		}

		@Override
		public String toString() {
			return "Person2 [age=" + age + ", personName=" + personName + ", kanaCode=" + kanaCode + "]";
		}
	}

	static class PersonName {

		private String firstname;
		private String lastname;

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		@Override
		public String toString() {
			return "PersonName [firstname=" + firstname + ", lastname=" + lastname + "]";
		}
	}

	static class Person {

		private int age;
		private String name;
		private char kanaCode;

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public char getKanaCode() {
			return kanaCode;
		}

		public void setKanaCode(char kanaCode) {
			this.kanaCode = kanaCode;
		}

		@Override
		public String toString() {
			return "Person [age=" + age + ", name=" + name + ", kanaCode=" + kanaCode + "]";
		}
	}
}
