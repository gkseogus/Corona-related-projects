package mqtt_project;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Timer;
import java.util.TimerTask;

public class MqttPublisher_API_project implements MqttCallback{ //implement callback(상속) 추가& 필요한 메소드 정의
	static MqttClient sampleClient; //Mqtt Client 객체 선언
	
    public static void main(String[] args) {
    	MqttPublisher_API_project obj = new MqttPublisher_API_project();
    	obj.run();
    }
    public void run() {  
    	connectBroker(); // 브로커 서버에 접속
    	try { 
    		sampleClient.subscribe("btn"); //btn topic subscribe
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	// 계속 반복하기 위한 while문
    	while(true) {
    		try {
    			String[] covid19_data  = get_covid19_data(); //보건복지부 API
    	       	String[] covid19hospital_data = get_covid19hospital_data(); // 건강보험심사평가원 API
    	       	// temp
    	       	String[] sutime = covid19_data[0].split(",");
    	       	String[] Death = covid19_data[1].split(",");
    	       	String[] Def = covid19_data[2].split(",");
    	       	String[] Isol = covid19_data[3].split(",");
    	       	
    	       	for (int i = 0; i < sutime.length; i++) {
    	       		System.out.println("---------------------------------------------");
    	       		publish_data("sutime", "{\"DateAndTime\": "+sutime[i]+"}"); // 데이터를 확인한 시간 데이터 발행
        	       	publish_data("Death", "{\"deceasedPatient\": "+Death[i]+"}"); // 사망자 수 데이터  발행
        	       	publish_data("Def", "{\"confirmedPatient\": "+Def[i]+"}"); //확진자 수 데이터 발행
        	       	publish_data("Isol", "{\"QuarantinePatient\": "+Isol[i]+"}"); //격리중 환자수 데이터 발행
        	       	publish_data("Sido1", "{\"동작구 코로나19 검사기관\": "+covid19hospital_data[0]+" "+covid19hospital_data[1]+"}"); //동작구 코로나19 검사기관 데이터 발행
        	       	publish_data("Sido2", "{\"강동구 코로나19 검사기관\": "+covid19hospital_data[2]+" "+covid19hospital_data[3]+"}"); //강동구 코로나19 검사기관 데이터 발행
        	       	publish_data("Sido3", "{\"강동구 코로나19 검사기관\": "+covid19hospital_data[4]+" "+covid19hospital_data[5]+"}"); //관악구 코로나19 검사기관 데이터 발행
        	       	Thread.sleep(10000); 
				}

    		}catch (Exception e) {
				// TODO: handle exception
    			try {
    				sampleClient.disconnect();
				} catch (MqttException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			e.printStackTrace();
    	        System.out.println("Disconnected");
    	        System.exit(0);
			}
    	}
    }
    
    public void connectBroker() {
        String broker = "tcp://127.0.0.1:1883"; //접속 할 브로커 서버의 주소 
        String clientId = "practice"; // 클라이언트 ID
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);//Mqtt Client 객체 초기화
            MqttConnectOptions connOpts = new MqttConnectOptions(); //접속시 접속의 옵션을 정의하는 객체 생성
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts); //브로커 서버에 접속
            sampleClient.setCallback(this);//Call back Option 추가
            System.out.println("Connected");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public void publish_data(String topic_input, String data) {
        String topic = topic_input; //토픽
        int qos = 0; //Qos level (0,2,3)
        try {
            System.out.println("Publishing message: "+data);
            sampleClient.publish(topic, data.getBytes(), qos, false);
            System.out.println("Message published");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
	//서울시 5월 한달 간 사망자, 확진자, 격리중인 사람 수 
    public String[] get_covid19_data() { 
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmmss"); 
    	System.out.println(d_format.format(current));
    	String date = d_format.format(current).substring(0,8); //실시간 날짜 저장
    	String time = d_format.format(current).substring(8,10); //실시간 시간 저장
    	
    	String url = "http://openapi.data.go.kr/openapi/service/rest/Covid19/getCovid19SidoInfStateJson"
    			+ "?serviceKey=6MAuV1Ni9N7pH8GR1D2%2FzleRYEVE9SQDA%2Bx8F5Uzo0Yl%2FqBdCjkYNtbVkxG6mkw%2BXIxD0ipjASwWvZVh1YM0LA%3D%3D" // 인증키
    			+ "&pageNo=1"
    			+ "&numOfRows=10"
    			+ "&startCreateDt=20210501"
    			+ "&endCreateDt=20210531";
    	
    	//데이터를 저장할 변수 초기화
 
    	Document doc = null;
    	String sutime = ""; //데이터 시간
    	String Death = ""; //사망자 수
    	String Def = "";  //확진자 수
    	String Isol = ""; //격리중 환자수
	
    	//Jsoup으로 API 데이터 가져오기
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		Elements elements = doc.select("item");
		for (Element e : elements) { //각각 ITEM을 조사하기 위해 
			if (e.select("gubunEn").text().equals("Seoul")) { //서울시 covid19 데이터
				sutime += e.select("createDt").text()+","; //시간 데이터
				Death += e.select("deathCnt").text()+","; //사망자 데이터
				Def += e.select("defCnt").text()+","; //확진자 데이터
				Isol += e.select("isolIngCnt").text()+","; //격리 환자 데이터
			}
		} 
		System.out.println("확인용" +sutime);
		String[] out1 = {sutime,Death,Def,Isol};
    	return out1;
    }
    
    //서울시 코로나검사 실시기관
    public String[] get_covid19hospital_data() { 
    	String url = "http://apis.data.go.kr/B551182/pubReliefHospService/getpubReliefHospList"
    			+ "?serviceKey=6MAuV1Ni9N7pH8GR1D2%2FzleRYEVE9SQDA%2Bx8F5Uzo0Yl%2FqBdCjkYNtbVkxG6mkw%2BXIxD0ipjASwWvZVh1YM0LA%3D%3D"
    			+ "&pageNo=1"
    			+ "&numOfRows=10"
    			+ "&spclAdmTyCd=97"; //수정
    	
    	//데이터를 저장할 변수 초기화
    	//서울시 코로나검사 실시기관
		String Sido1 = ""; 
		String Sido2 = ""; 
		String Sido3 = ""; 
		
		//서울시 병원 번호
		String tel1 = ""; 
		String tel2 = ""; 
		String tel3 = ""; 
    	Document doc = null;
		
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//서울시 코로나검사 실시기관
		Elements elements = doc.select("item");
		for (Element e : elements) {
			if (e.select("sgguNm").text().equals("동작구")) { 
				Sido1 = e.select("yadmNm").text();
				tel1 = e.select("telno").text();
			}
			if (e.select("sgguNm").text().equals("강동구")) { 
				Sido2 = e.select("yadmNm").text();
				tel2 = e.select("telno").text();
			}
			if (e.select("sgguNm").text().equals("관악구")) { 
				Sido3 = e.select("yadmNm").text();
				tel3 = e.select("telno").text();
			}
		}
		String[] out2 = {Sido1,tel1, Sido2,tel2, Sido3,tel3};
    	return out2;
    }
    
    ///@@@@@@@@@@@@@@@@@
	@Override
	public void connectionLost(Throwable arg0) { //연결이 끊어지면 ...
		// TODO Auto-generated method stub
		System.out.println("연결이 끊김");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception { //topic에 따라 동작이 달라져야 되기에 topic을 확인
		// TODO Auto-generated method stub
		if (topic.equals("btn")){ //topic이 led이면 아래 소스 수행
			System.out.println("--------------------Actuator Function--------------------");
			System.out.println("버튼 눌림");
			System.out.println("btn: " + msg.toString());
			// 라즈베리파이, 아두이노 제어소스코드 들어가면 실제로 제어 가능
			System.out.println("---------------------------------------------------------");
		}		
	}
}

// written by Sang-woo Lee (http://210.115.227.106:8080/cv_sw)