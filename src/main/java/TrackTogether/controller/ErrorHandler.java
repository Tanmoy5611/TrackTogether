package TrackTogether.controller;

import TrackTogether.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFoundException(HttpServletRequest req, NotFoundException e) {

        if (isApiRequest(req)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(404, e.getMessage()));
        }

        ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.addObject("message", e.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(HttpServletRequest req, Exception e) {

        if (isApiRequest(req)) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDto(500, "An unexpected error occurred"));
        }

        ModelAndView modelAndView = new ModelAndView("error/500");
        modelAndView.addObject("message", e.getMessage());
        return modelAndView;
    }

    private boolean isApiRequest(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/api");
    }

    private record ErrorDto(int status, String message) {}
}