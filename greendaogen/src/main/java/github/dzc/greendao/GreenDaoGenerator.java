package github.dzc.greendao;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

/**
 * Created by dzc on 15/11/21.
 */
public class GreenDaoGenerator {
    public static void main(String[] args) throws Exception {
//        Schema schema = new Schema(1,"git.dzc.downloadmanagerlib.download");
//        Entity entity = schema.addEntity("DownloadDBEntity");
//        entity.setClassNameDao("DownloadDao");
//        entity.setTableName("download");
//        entity.addStringProperty("downloadId").primaryKey();
//        entity.addLongProperty("toolSize");
//        entity.addLongProperty("completedSize");
//        entity.addStringProperty("url");
//        entity.addStringProperty("saveDirPath");
//        entity.addStringProperty("fileName");
//        entity.addIntProperty("downloadStatus");
//        new DaoGenerator().generateAll(schema,"你的路径/downloadmanagerlib/src/main/java/");
        String s = "8h2fmF2%5258%kc368-2EltFii1F2EF273ecb2814-lt%l.%2F%31_Fya8f148%p2ec2%353_la%f224%%5%F.oF5%E22.u3181155E3mxm1E53%3mtD2b4%EE-A5i%81E455ph19d658%n%.a2%8%%E53_fd76E95u";
        xiami(s);
    }
    public static void xiami(String str){
        int a1 = str.indexOf(0);
        String a2 = str.substring(1);
        double a3 = Math.floor(a2.length()/a1);
        int a4 = a2.length()/a1;
        int a6 = 0;
        String a7 = "";
        String a8 = "";
        String[] a5 = new String[a1>a4?a1:a4];
        for(;a6<a4;a6++){
            a5[a6] = a2.substring((int)(a3+1)*a6,(int)(a3+1));
        }
        for(;a6<a1;a6++){
            a5[a6] = a2.substring((int)(a3*(a6-a4)+(a3+1)*a4),(int)a3);
        }
        for(int i=0,a50L = a5[0].length();i<a50L;i++){
            for(int j=0,a5L = a5.length;j<a5L;j++){
                a7 += a5[j].charAt(i);
            }
        }
        System.out.println(a7);

//                a7 = decodeURIComponent(a7);
//                for (var i = 0,a7_length = a7.length; i < a7_length; ++i) {
//                    a8 += a7.charAt(i) === '^' ? '0': a7.charAt(i);
//                }
//                return a8;
//            } catch(e) {
//                return false;
//            }
//        }
//        var s=getMp3Location("8h2fmF2%5258%kc368-2EltFii1F2EF273ecb2814-lt%l.%2F%31_Fya8f148%p2ec2%353_la%f224%%5%F.oF5%E22.u3181155E3mxm1E53%3mtD2b4%EE-A5i%81E455ph19d658%n%.a2%8%%E53_fd76E95u") ;
    }
}
