package com.example.bookstoreuserservice.auth.dto;

public class IntrospectResponse {
    private Result result;

    public IntrospectResponse() {}

    public IntrospectResponse(boolean valid) {
        this.result = new Result(valid);
    }

    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }

    public static class Result {
        private boolean valid;
        
        public Result() {}
        public Result(boolean valid) { this.valid = valid; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
    }
}
