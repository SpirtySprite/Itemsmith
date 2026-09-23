package com.kirugoldzzzz.itemsmith;

final class EditException extends RuntimeException {

    EditException(String reason) {
        super(reason, null, false, false);
    }
}
