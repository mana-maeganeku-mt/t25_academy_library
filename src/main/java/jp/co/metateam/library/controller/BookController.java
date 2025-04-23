package jp.co.metateam.library.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.service.BookMstService;
import lombok.extern.log4j.Log4j2;

/**
 * 書籍関連クラス
 */
@Log4j2
@Controller
public class BookController {

    private final BookMstService bookMstService;

    @Autowired
    public BookController(BookMstService bookMstService) {
        this.bookMstService = bookMstService;
    }

    @GetMapping("/book/index")
    public String index(Model model) {
        // 書籍を全件取得
        List<BookMstDto> bookMstList = this.bookMstService.findAvailableWithStockCount();

        model.addAttribute("bookMstList", bookMstList);

        return "book/index";
    }

    @GetMapping("/book/add")
    public String add(Model model) {
        if (!model.containsAttribute("bookMstDto")) {
            model.addAttribute("bookMstDto", new BookMstDto());
        }
        return "book/add";
    }

    @PostMapping("/book/add")
    public String addBook(@Valid @ModelAttribute BookMstDto bookMstDto, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("bookMstDto", bookMstDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.bookMstDto", result);
            return "redirect:/book/add";
        }

        boolean errTitleFlg = false;
        boolean errIsbnFlg = false;

        List<String> errorMessages = new ArrayList<>();  // エラーメッセージのリスト

        if (bookMstDto.getTitle()== null || bookMstDto.getTitle().isEmpty()) {
            // どっちかがtrueなら、この中の処理が実行される
            errorMessages.add("書籍名は必須です。");
            //System.out.println("書籍名は必須です");
            result.rejectValue("title", "error.required", "書籍名は必須です");
            errTitleFlg = true;
        }
 
        if (bookMstDto.getIsbn()== null || bookMstDto.getIsbn().isEmpty()) {
            // どっちかがtrueなら、この中の処理が実行される
            errorMessages.add("ISBNは必須です");
           // System.out.println("ISBNは必須です");
           result.rejectValue("isbn", "error.required", "ISBNは必須です");
            errIsbnFlg = true;
        }

        if (bookMstDto.getTitle().length() >256) {
            errorMessages.add("書籍名は255文字以内で入力してください");
            //System.out.println("書籍名は255文字以内で入力してください");
            result.rejectValue("title", "error.maxlength", "書籍名は255文字以内で入力してください");
            errTitleFlg = true;
            }

        if (bookMstDto.getIsbn().length()!= 13) {
            errorMessages.add("ISBNは13桁で入力してください");
            //System.out.println("ISBNは13桁で入力してください");
            result.rejectValue("isbn", "error.length", "ISBNは13桁で入力してください");
                errIsbnFlg = true;
            }

            //ISBNが半角数字以外→半角数字のみ→ISBNは半角数字で入力してください
         if (!bookMstDto.getIsbn().matches("^[0-9]+$")) {
                errorMessages.add("ISBNは半角数字で入力してください");
                result.rejectValue("isbn", "error.length", "ISBNは半角数字で入力してください");
                errIsbnFlg = true;  // ISBNが半角でない場合、エラーフラグを立てる
                //System.out.println("ISBNは半角数字で入力してください");
            }
   
        // ISBNの重複チェックだけコントローラで追加
        if (bookMstService.selectByIsbn(bookMstDto.getIsbn().trim()) != null) {
            result.rejectValue("isbn", "error.exists", "登録済みのISBNです");
            ra.addFlashAttribute("bookMstDto", bookMstDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.bookMstDto", result);
            return "book/add";
        }
   
     //エラーがあれば、エラーメッセージリストにフラッシュ属性に渡す
     if (errTitleFlg || errIsbnFlg){
        ra.addFlashAttribute("errorMessages", errorMessages);//// エラーメッセージのリストをフラッシュ属性として渡す
        return "book/add";  // エラーがある場合、フォーム画面にリダイレクト
}

        bookMstService.save(bookMstDto);
        ra.addFlashAttribute("message", "書籍が正常に登録されました");
        return "redirect:/book/index";
    }
}
