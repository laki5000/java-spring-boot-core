package com.example.project.controller.v1;

import com.example.generated.api.DemoApi;
import com.example.generated.dto.DemoResponse;
import com.example.project.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController implements DemoApi {

  private final DemoService demoService;

  @Override
  public ResponseEntity<DemoResponse> getDemo(Boolean error) {
    return ResponseEntity.ok(new DemoResponse(demoService.getDemoMessage(error)));
  }
}
