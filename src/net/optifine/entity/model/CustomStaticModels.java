package net.optifine.entity.model;

import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.TridentModel;

public class CustomStaticModels {
    private static ParrotModel parrotModel;
    private static BookModel bookModel;
    private static TridentModel tridentModel;
    private static ShieldModel shieldModel;

    public static void clear() {
        parrotModel = null;
        bookModel = null;
        tridentModel = null;
        shieldModel = null;
    }

    public static void setParrotModel(ParrotModel model) {
        parrotModel = model;
    }

    public static ParrotModel getParrotModel() {
        return parrotModel;
    }

    public static void setBookModel(BookModel model) {
        bookModel = model;
    }

    public static BookModel getBookModel() {
        return bookModel;
    }

    public static void setTridentModel(TridentModel tridentModel) {
        CustomStaticModels.tridentModel = tridentModel;
    }

    public static TridentModel getTridentModel() {
        return tridentModel;
    }

    public static ShieldModel getShieldModel() {
        return shieldModel;
    }

    public static void setShieldModel(ShieldModel shieldModel) {
        CustomStaticModels.shieldModel = shieldModel;
    }
}