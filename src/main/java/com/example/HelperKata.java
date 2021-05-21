package com.example;


import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class HelperKata {
    private static final  String EMPTY_STRING = "";
    private static String ANTERIOR_BONO = null;


    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {

        return separarCaracteres(createFluxFrom(fileBase64));

    }

    private static Flux<CouponDetailDto> separarCaracteres(Flux<String > lineaASeparar){
        AtomicInteger counter = new AtomicInteger(0);
        String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();
        Set<String> codes = new HashSet<>();
            return lineaASeparar.skip(1)
                            .map(line -> getTupleOfLine(line, line.split(characterSeparated), characterSeparated))
                            .map(tuple -> validarErroresDelBono(counter,codes,tuple));
    }

    private static CouponDetailDto validarErroresDelBono(AtomicInteger counter, Set<String> codes, Tuple2<String, String> tuple){
        String dateValidated = null;
        String errorMessage;
        String bonoForObject;
        String bonoEnviado;

        errorMessage = fileError(codes, tuple);
        if (errorMessage.equals(null)){
            dateValidated = tuple.getT2();
        }

        bonoEnviado = tuple.getT1();
        bonoForObject = bonoIsSend(bonoEnviado);

        return CouponDetailDto.aCouponDetailDto()
                .withCode(bonoForObject)
                .withDueDate(dateValidated)
                .withNumberLine(counter.incrementAndGet())
                .withMessageError(errorMessage)
                .withTotalLinesFile(1)
                .build();
    }

    private static String fileError(Set<String> codes, Tuple2<String, String> tuple) {

        Map<String, Boolean> error = new LinkedHashMap<String, Boolean>();

        error.put(ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString(),compara2Objetos(tuple));
        error.put(ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString(),!codes.add(tuple.getT1()));
        error.put(ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString(),!validateDateRegex(tuple.getT2()));
        error.put(ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString(),validateDateIsMinor(tuple.getT2()));

        for (Map.Entry<String, Boolean> bonoError :
                error.entrySet()) {
            if (bonoError.getValue()){
                return bonoError.getKey();
            }
        }

        return null;
    }

    private static boolean compara2Objetos(Tuple2<String, String> tuple) {
        return tuple.getT1().isBlank() || tuple.getT2().isBlank();
    }

    private static String bonoIsSend(String bonoEnviado) {

        String bonoForObject = null;

        if (isBonoNull(bonoEnviado)) {
            ANTERIOR_BONO = typeBono(bonoEnviado);
            bonoForObject = bonoEnviado;
        }

        return bonoForObject;
    }

    private static boolean isBonoNull(String bonoEnviado) {
        return ANTERIOR_BONO == null || ANTERIOR_BONO.equals(typeBono(bonoEnviado));
    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(
                () -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines(),
                Flux::fromStream,
                Stream::close
        );
    }

    public static String typeBono(String bonoIn) {
      return  validateEan13(bonoIn)
              ? ValidateCouponEnum.EAN_13.getTypeOfEnum()
               : validateAlphanumeric(bonoIn);
    }

    private static String validateAlphanumeric(String bonoIn) {
        return validateEan39(bonoIn) ? ValidateCouponEnum.EAN_39.getTypeOfEnum()
                : ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum();
    }

    private static boolean validateEan39(String bonoIn) {
        return bonoIn.startsWith("*")
                && tamañoCodigoBonoMayorQue(bonoIn.replace("*", "").length(), 1)
                && tamañoCodigoBonoMenorQue(bonoIn.replace("*", "").length(),43);
    }

    private static boolean validateEan13(String bonoIn) {
        return bonoIn.chars().allMatch(Character::isDigit)
                  && tamañoCodigoBonoMayorQue(bonoIn.length(), 12)
                  && tamañoCodigoBonoMenorQue(bonoIn.length(), 13);
    }

    private static boolean tamañoCodigoBonoMayorQue(int bonoLength, int number){
        return bonoLength >= number;
    }


    private static boolean tamañoCodigoBonoMenorQue(int bonoLength, int number){
        return bonoLength <= number;
    }

    public static boolean validateDateRegex(String dateForValidate) {
        String regex = FileCSVEnum.PATTERN_DATE_DEFAULT.getId();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dateForValidate);
        return matcher.matches();
    }

    private static byte[] decodeBase64(final String fileBase64) {
        return Base64.getDecoder().decode(fileBase64);

    }

    private static Tuple2<String, String> getTupleOfLine(String line, String[] array, String characterSeparated) {
        return Objects.isNull(array) || array.length == 0
                ? Tuples.of(EMPTY_STRING, EMPTY_STRING)
                : comparaTuplas2(line, array, characterSeparated);
    }

    private static Tuple2<String, String> comparaTuplas2(String line, String[] array, String characterSeparated) {
        return array.length < 2
        ? comparaTuplas(line, array, characterSeparated)
        : Tuples.of(array[0], array[1]);
    }

    private static Tuple2<String, String> comparaTuplas(String line, String[] array, String characterSeparated) {
        return line.startsWith(characterSeparated)
                ? Tuples.of(EMPTY_STRING, array[0])
                : Tuples.of(array[0], EMPTY_STRING);
    }

    public static boolean validateDateIsMinor(String dateForValidate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FileCSVEnum.PATTERN_SIMPLE_DATE_FORMAT.getId());
            Date dateActual = sdf.parse(sdf.format(new Date()));
            Date dateCompare = sdf.parse(dateForValidate);
            return tamañoCodigoBonoMenorQue(dateCompare.compareTo(dateActual),0 );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
