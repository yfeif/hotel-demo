package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class HotelDocumentTest {

    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

    @Test
    void testAddDocument() throws IOException {
        // 1.查询数据库hotel数据
        Hotel hotel = hotelService.getById(61083L);
        // 2.转换为HotelDoc
        HotelDoc hotelDoc = new HotelDoc(hotel);
        // 3.转JSON
        String json = JSON.toJSONString(hotelDoc);

        // 1.准备Request
//        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        // 1.准备Request (使用低级别客户端以避免解析错误)
        Request request = new Request("PUT", "/hotel/_doc/" + hotelDoc.getId().toString());
        request.setJsonEntity(json);
        request.addParameter("timeout", "1m");
        // 2.准备请求参数DSL，其实就是文档的JSON字符串
//        request.source(json, XContentType.JSON);
        // 3.发送请求
//        client.index(request, RequestOptions.DEFAULT);
        Response response = client.getLowLevelClient().performRequest(request);
        String responseString = EntityUtils.toString(response.getEntity());
        System.out.println("response = " + responseString);
    }

    @Test
    void testGetDocumentById() throws IOException {
        // 1.准备Request      // GET /hotel/_doc/{id}
        GetRequest request = new GetRequest("hotel", "61083");
        // 2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3.解析响应结果
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println("hotelDoc = " + hotelDoc);
    }

    @Test
    void testDeleteDocumentById() throws IOException {
        // 1.准备Request      // DELETE /hotel/_doc/{id}
//        DeleteRequest request = new DeleteRequest("hotel", "61083");
        Request request = new Request("DELETE", "/hotel/_doc/61083");
        // 2.发送请求
//        client.delete(request, RequestOptions.DEFAULT);
        Response response = client.getLowLevelClient().performRequest(request);
        String responseString = EntityUtils.toString(response.getEntity());
        System.out.println("response = " + responseString);
    }

    @Test
    void testUpdateById() throws IOException {
        // 1.准备Request
//        UpdateRequest request = new UpdateRequest("hotel", "61083");
        Request request = new Request("POST", "/hotel/_update/61083");
        // 2.准备参数
//        request.doc(
//                "price", "870"
//        );
        String json = "{\"doc\": {\"price\": \"870\"}}";
        request.setJsonEntity(json);

        // 3.发送请求
//        client.update(request, RequestOptions.DEFAULT);
        Response response = client.getLowLevelClient().performRequest(request);
        String responseString = EntityUtils.toString(response.getEntity());
        System.out.println("update response = " + responseString);

    }

    @Test
    void testBulkRequest() throws IOException {
        // 查询所有的酒店数据
        List<Hotel> list = hotelService.list();

        // 1.准备Request
//        BulkRequest request = new BulkRequest();
        Request request = new Request("POST", "/_bulk");
        StringBuilder bulkRequest = new StringBuilder();

        // 2.准备参数
        for (Hotel hotel : list) {
            // 2.1.转为HotelDoc
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 2.2.转json
            String json = JSON.toJSONString(hotelDoc);
            bulkRequest.append("{ \"index\" : { \"_index\" : \"hotel\", \"_id\" : \"").append(hotel.getId()).append("\" } }\n");
            bulkRequest.append(json).append("\n");

            // 2.3.添加请求
//            request.add(new IndexRequest("hotel").id(hotel.getId().toString()).source(json, XContentType.JSON));

        }
        // 3.发送请求
        request.setJsonEntity(bulkRequest.toString());
        Response response = client.getLowLevelClient().performRequest(request);
        String responseString = EntityUtils.toString(response.getEntity());
        System.out.println("bulk response = " + responseString);
//        client.bulk(request, RequestOptions.DEFAULT);
    }

    @BeforeEach
    void setUp() {
        RestClientBuilder builder = RestClient.builder(
                HttpHost.create("http://192.168.147.130:9200")
        );
        // 配置基本身份验证（用户名/密码）
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "MyStrongPassword123!")
        );

        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
        );

        client = new RestHighLevelClient(builder);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }



}
