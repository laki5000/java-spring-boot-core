package com.example.proj.controller.v1;

import com.example.generated.api.DemoApi;
import com.example.generated.dto.ApiResponseString;
import com.example.proj.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController implements DemoApi {

  private final DemoService demoService;

  @Override
  public ResponseEntity<ApiResponseString> getDemo(Boolean error) {
    ApiResponseString response = new ApiResponseString();
    response.setData(demoService.getDemoMessage(error));

    return ResponseEntity.ok(response);
  }
}
