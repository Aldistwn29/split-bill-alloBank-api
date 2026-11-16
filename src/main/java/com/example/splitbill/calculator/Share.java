package com.example.splitbill.calculator;

import java.math.BigDecimal;

import com.example.splitbill.model.Participant;

public record Share(Participant participant, BigDecimal shareAmount) {
}