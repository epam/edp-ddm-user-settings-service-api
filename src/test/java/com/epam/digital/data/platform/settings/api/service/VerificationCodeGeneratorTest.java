package com.epam.digital.data.platform.settings.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.settings.api.service.impl.VerificationCodeGeneratorImpl;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationCodeGeneratorTest {

  @Mock
  private SecureRandom mockRandomGenerator;

  private VerificationCodeGenerator codeGeneratorService;

  @BeforeEach
  public void beforeEach() {
    codeGeneratorService = new VerificationCodeGeneratorImpl(mockRandomGenerator);
  }

  @Test
  void shouldGenerateRandomValue() {
    when(mockRandomGenerator.nextInt(anyInt())).thenReturn(123456);

    String generatedCode = codeGeneratorService.generate();

    var boundCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(mockRandomGenerator).nextInt(boundCaptor.capture());

    assertThat(boundCaptor.getValue()).isEqualTo(999999);
    assertThat(generatedCode).isEqualTo("123456");
  }

  @Test
  void shouldFormatValueWithLeadingZeroes() {
    when(mockRandomGenerator.nextInt(anyInt())).thenReturn(5);

    String generatedCode = codeGeneratorService.generate();

    assertThat(generatedCode).isEqualTo("000005");
  }
}
