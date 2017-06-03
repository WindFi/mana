package me.sunzheng.mana.home.onair.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Locale;

/**
 * e.g:
 * "bangumi_moe": "[{\"_id\":\"58df790679c3a0541f4b61b3\",\"name\":\"フレームアームズ・ガール\",\"type\":\"bangumi\",\"synonyms\":[\"フレームアームズ・ガール\",\"Frame Arms Girl 機甲少女\",\"Frame Arms Girl 骨裝機娘\",\"Frame Arms Girl\",\"FRAME ARMS GIRL\",\"Frame Arms Girl 机甲少女\"],\"locale\":{\"ja\":\"フレームアームズ・ガール\",\"zh_tw\":\"Frame Arms Girl 機甲少女\",\"en\":\"Frame Arms Girl\",\"zh_cn\":\"FRAME ARMS GIRL\"},\"syn_lowercase\":[\"フレームアームズ・ガール\",\"frame arms girl 機甲少女\",\"frame arms girl 骨裝機娘\",\"frame arms girl\",\"frame arms girl\",\"frame arms girl 机甲少女\"],\"activity\":0},{\"_id\":\"548ee0ea4ab7379536f56354\",\"activity\":0,\"locale\":{\"en\":\"chs\"},\"name\":\"简体中文\",\"syn_lowercase\":[\"简体中文\",\"简体\",\"chs\",\"gb\",\"简\",\"簡\",\"gb简体\",\"简体内嵌\"],\"synonyms\":[\"简体中文\",\"简体\",\"chs\",\"GB\",\"简\",\"簡\",\"GB简体\",\"简体内嵌\"],\"type\":\"lang\"}]",
 * Created by Sun on 2017/5/24.
 */
//todo source from bgm.tv
public class BangumiMoe {

    @SerializedName("_id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("synonyms")
    @Expose
    private List<String> synonyms = null;
    @SerializedName("locale")
    @Expose
    private Locale locale;
    @SerializedName("syn_lowercase")
    @Expose
    private List<String> synLowercase = null;
    @SerializedName("activity")
    @Expose
    private Integer activity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public List<String> getSynLowercase() {
        return synLowercase;
    }

    public void setSynLowercase(List<String> synLowercase) {
        this.synLowercase = synLowercase;
    }

    public Integer getActivity() {
        return activity;
    }

    public void setActivity(Integer activity) {
        this.activity = activity;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
