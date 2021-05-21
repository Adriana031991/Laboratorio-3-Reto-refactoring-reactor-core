package com.example;


import reactor.core.publisher.Flux;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class HelperKata {
    private static final String EMPTY_STRING = "";
    private static final String CHARACTER_SEPARATED = FileCSVEnum.CHARACTER_DEFAULT.getId();
    private static final Set<String> codes = new HashSet<>();
    private static AtomicInteger counter = new AtomicInteger(0);

    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {
        return createFluxFrom(fileBase64)
                .map(line -> separarCaracteres(line))
                .map(modelCoupon -> validaColumnaVacia(modelCoupon))
                .map(couponDetailDto -> validaCodigoDuplicado(couponDetailDto))
                .map(couponDetailDto1 -> validaFecha(couponDetailDto1));
    }

    public static CouponDetailDto validaColumnaVacia(ModelCoupon modelCoupon){
        return Optional.of(modelCoupon)
                .filter(coupon -> coupon.getCodigo().isBlank()|| coupon.getFecha().isBlank())
                .map(modelCoupon1 -> CouponDetailDto.aCouponDetailDto()
                        .withCode(null)
                        .withDueDate(null)
                        .withNumberLine(counter.incrementAndGet())
                        .withTotalLinesFile(1)
                        .withMessageError(ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString()))
                .orElseGet(() -> CouponDetailDto.aCouponDetailDto()
                        .withCode(modelCoupon.getCodigo())
                        .withDueDate(modelCoupon.getFecha())
                        .withNumberLine(counter.incrementAndGet())
                        .withTotalLinesFile(1)
                        .withMessageError(""));

    }

    private static ModelCoupon separarCaracteres(String lineaASeparar) {

        var columns = List.of(lineaASeparar.split(CHARACTER_SEPARATED));

        return Optional.of(columns)
                .filter(HelperKata::hasAllColumns)
                .map(columnFile -> new ModelCoupon(columnFile.get(0), columnFile.get(1)))
                //.map(line -> getTupleOfLine(line, line.split(characterSeparated), characterSeparated))
                .orElseGet(() -> validaColumnaVacia(lineaASeparar));
    }

    private static ModelCoupon validaColumnaVacia(String lineaASeparar) {
        var columns = List.of(lineaASeparar.split(CHARACTER_SEPARATED));
        return Optional.of(lineaASeparar)
                .filter(HelperKata::hasCode)
                .map(lineWithCode -> new ModelCoupon(columns.get(0),EMPTY_STRING))
                .orElseGet(() -> new ModelCoupon(EMPTY_STRING,columns.get(0)));
    }

    private static CouponDetailDto validaCodigoDuplicado(CouponDetailDto couponDetailDto){
        return Optional.ofNullable(couponDetailDto.getCode())
                .filter(code -> !codes.add(code))
                .map(c -> couponDetailDto
                        .withMessageError(ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString()))
                .orElseGet(() -> couponDetailDto);
    }


    private static CouponDetailDto validaFecha(CouponDetailDto couponDetailDto){
        return Optional.ofNullable(couponDetailDto.getDueDate())
                .filter(date -> !validateDateRegex(date))
                .map(date -> couponDetailDto.withDueDate(null))
                .filter(coupon -> couponDetailDto.getMessageError().isBlank())
                .map(c -> couponDetailDto
                        .withMessageError(ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString()))
                .orElseGet(() -> validateDateIsMinor(couponDetailDto));
    }

    private static CouponDetailDto validateDateIsMinor(CouponDetailDto couponDetailDto) {
        return Optional.ofNullable(couponDetailDto.getDueDate())
                .filter(HelperKata::validateDateIsMinor)
                .map(date -> couponDetailDto.withDueDate(null))
                .filter(coupon -> coupon.getMessageError().isBlank())
                .map(couponDetailDto1 -> couponDetailDto.withMessageError(ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString())
                        .build())
                .orElseGet(() -> couponDetailDto.build());

    }


    private static boolean hasAllColumns(List<String> columns) {
        return columns
                .stream()
                .noneMatch(String::isBlank);
    }

    private static boolean hasCode(String line) {
        return !line.startsWith(CHARACTER_SEPARATED);
    }


    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(
                () -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines().skip(1),
                Flux::fromStream,
                Stream::close
        );
    }

    public static String typeBono(String bonoIn) {
        return validateEan13(bonoIn)
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
                && tamañoCodigoBonoMenorQue(bonoIn.replace("*", "").length(), 43);
    }

    private static boolean validateEan13(String bonoIn) {
        return bonoIn.chars().allMatch(Character::isDigit)
                && tamañoCodigoBonoMayorQue(bonoIn.length(), 12)
                && tamañoCodigoBonoMenorQue(bonoIn.length(), 13);
    }

    private static boolean tamañoCodigoBonoMayorQue(int bonoLength, int number) {
        return bonoLength >= number;
    }


    private static boolean tamañoCodigoBonoMenorQue(int bonoLength, int number) {
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

    public static boolean validateDateIsMinor(String dateForValidate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FileCSVEnum.PATTERN_SIMPLE_DATE_FORMAT.getId());
            Date dateActual = sdf.parse(sdf.format(new Date()));
            Date dateCompare = sdf.parse(dateForValidate);
            return tamañoCodigoBonoMenorQue(dateCompare.compareTo(dateActual), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
