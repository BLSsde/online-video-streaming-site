package com.stream.app.spring_stream_backend;

import com.stream.app.spring_stream_backend.services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringStreamBackendApplicationTests {

	@Autowired
	VideoService videoService;

	@Test
	void contextLoads() {
		videoService.processVideo("0a9ad8b5-a62a-49cc-ac92-6c59075e854e");
	}

}
