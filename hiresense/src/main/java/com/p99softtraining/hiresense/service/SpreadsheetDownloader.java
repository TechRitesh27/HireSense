package com.p99softtraining.hiresense.service;

import java.io.IOException;
import java.io.InputStream;

public interface SpreadsheetDownloader {

    boolean supports(String url);

    InputStream download(String url) throws IOException;
}
