package com.epam.digital.data.platform.settings.api.utils;

public enum Header {
  X_ACCESS_TOKEN("X-Access-Token");

  private final String headerName;

  Header(String headerName) {
    this.headerName = headerName;
  }

  public String getHeaderName() {
    return headerName;
  }
}
