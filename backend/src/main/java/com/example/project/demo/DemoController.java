package com.example.project.demo;

import com.example.generated.api.DemoApi;
import com.example.generated.dto.ApiResponseString;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController implements DemoApi {

  private final DemoService demoService;

  @Override
  public ResponseEntity<ApiResponseString> getDemo(Boolean error) {
    return ResponseEntity.ok(new ApiResponseString(demoService.getDemoMessage(error)));
  }
}
