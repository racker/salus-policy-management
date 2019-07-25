/*
 * Copyright 2019 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.policy.manage.web.controller;

import com.rackspace.salus.telemetry.errors.AlreadyExistsException;
import com.rackspace.salus.telemetry.model.NotFoundException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice(basePackages = "com.rackspace.salus.monitor_management.web")
@ResponseBody
public class RestExceptionHandler extends
    com.rackspace.salus.common.web.AbstractRestExceptionHandler {

  @Autowired
  public RestExceptionHandler(ErrorAttributes errorAttributes) {
    super(errorAttributes);
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<?> handleBadRequest(
      HttpServletRequest request) {
    return respondWith(request, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({NotFoundException.class})
  public ResponseEntity<?> handleNotFound(
      HttpServletRequest request) {
    return respondWith(request, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({AlreadyExistsException.class})
  public ResponseEntity<?> handleAlreadyExists(
      HttpServletRequest request) {
    return respondWith(request, HttpStatus.UNPROCESSABLE_ENTITY);
  }

}
