/*
 * Copyright (C) 2015 Arthur Gregorio, AG.Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.com.webbudget.application.controller.entries;

import br.com.webbudget.application.controller.AbstractBean;
import br.com.webbudget.domain.entity.wallet.Wallet;
import br.com.webbudget.domain.entity.wallet.WalletBalance;
import br.com.webbudget.domain.entity.wallet.WalletType;
import br.com.webbudget.domain.misc.ex.InternalServiceError;
import br.com.webbudget.domain.misc.table.AbstractLazyModel;
import br.com.webbudget.domain.misc.table.Page;
import br.com.webbudget.domain.misc.table.PageRequest;
import br.com.webbudget.domain.service.WalletService;
import java.util.List;
import java.util.Map;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.exception.ConstraintViolationException;
import org.primefaces.model.SortOrder;

/**
 * Controller para a view do manutencao de carteiras
 *
 * @author Arthur Gregorio
 *
 * @version 1.2.0
 * @since 1.0.0, 04/03/2014
 */
@Named
@ViewScoped
public class WalletBean extends AbstractBean {

    @Getter
    @Setter
    private WalletBalance selectedBalance;
    
    @Getter
    private Wallet wallet;

    @Getter
    private List<WalletBalance> walletBalances;

    @Inject
    private WalletService walletService;

    @Getter
    private final AbstractLazyModel<Wallet> walletsModel;

    /**
     * 
     */
    public WalletBean() {
        
        this.walletsModel = new AbstractLazyModel<Wallet>() {
            @Override
            public List<Wallet> load(int first, int pageSize, String sortField, 
                    SortOrder sortOrder, Map<String, Object> filters) {
                
                final PageRequest pageRequest = new PageRequest();
                
                pageRequest
                        .setFirstResult(first)
                        .withPageSize(pageSize)
                        .sortingBy(sortField, "inclusion")
                        .withDirection(sortOrder.name());
                
                final Page<Wallet> page = walletService.listWalletsLazily(null, pageRequest);
                
                this.setRowCount(page.getTotalPagesInt());
                
                return page.getContent();
            }
        };
    }
    
    /**
     *
     */
    public void initializeListing() {
        this.viewState = ViewState.LISTING;
    }

    /**
     * @param walletId
     */
    public void initializeForm(long walletId) {

        if (walletId == 0) {
            this.viewState = ViewState.ADDING;
            this.wallet = new Wallet();
        } else {
            this.viewState = ViewState.EDITING;
            this.wallet = this.walletService.findWalletById(walletId);
        }
    }

    /**
     * @param walletId
     */
    public void initializeAdjustment(long walletId) {
        this.wallet = this.walletService.findWalletById(walletId);
    }

    /**
     * @return o form de inclusao
     */
    public String changeToAdd() {
        return "formWallet.xhtml?faces-redirect=true";
    }

    /**
     * @return
     */
    public String changeToListing() {
        return "listWallets.xhtml?faces-redirect=true";
    }

    /**
     * @param walletId
     * @return
     */
    public String changeToEdit(long walletId) {
        return "formWallet.xhtml?faces-redirect=true&walletId=" + walletId;
    }

    /**
     * @param walletId
     * @return
     */
    public String changeToAdjustment(long walletId) {
        return "formAdjustment.xhtml?faces-redirect=true&walletId=" + walletId;
    }

    /**
     * @param walletId
     */
    public void changeToDelete(long walletId) {
        this.wallet = this.walletService.findWalletById(walletId);
        this.updateAndOpenDialog("deleteWalletDialog", "dialogDeleteWallet");
    }

    /**
     * @return
     */
    public String doCancel() {
        return "listWallets.xhtml?faces-redirect=true";
    }

    /**
     *
     */
    public void doSave() {

        try {
            this.walletService.saveWallet(this.wallet);
            this.wallet = new Wallet();

            this.addInfo(true, "wallet.saved");
        } catch (InternalServiceError ex) {
            this.addError(true, ex.getMessage(), ex.getParameters());
        } catch (Exception ex) {
            this.addError(true, "error.undefined-error", ex.getMessage());
        }
    }

    /**
     *
     */
    public void doAdjustment() {

        try {
            this.walletService.adjustBalance(this.wallet);
            this.updateAndOpenDialog("adjustmentDialog", "dialogAdjustment");
        } catch (InternalServiceError ex) {
            this.addError(true, ex.getMessage(), ex.getParameters());
        } catch (Exception ex) {
            this.addError(true, "error.undefined-error", ex.getMessage());
        } finally {
            this.updateComponent("walletsList");
        }
    }

    /**
     *
     */
    public void doUpdate() {

        try {
            this.wallet = this.walletService.updateWallet(this.wallet);

            this.addInfo(true, "wallet.updated");
        } catch (InternalServiceError ex) {
            this.addError(true, ex.getMessage(), ex.getParameters());
        } catch (Exception ex) {
            this.addError(true, "error.undefined-error", ex.getMessage());
        }
    }

    /**
     *
     */
    public void doDelete() {

        try {
            this.walletService.deleteWallet(this.wallet);
            this.addInfo(true, "wallet.deleted");
        } catch (InternalServiceError ex) {
            this.addError(true, ex.getMessage(), ex.getParameters());
        } catch (Exception ex) {
            if (this.containsException(ConstraintViolationException.class, ex)) {
                this.addError(true, "error.wallet.integrity-violation", 
                        this.wallet.getFriendlyName());
            } else {
                this.addError(true, "error.undefined-error", ex.getMessage());
            }
        } finally {
            this.updateComponent("walletsList");
            this.closeDialog("dialogDeleteWallet");
        }
    }
    
    /**
     * @param walletId 
     */
    public void showBalance(long walletId) {

        this.wallet = this.walletService.findWalletById(walletId);
        this.walletBalances = this.walletService.listBalancesByWallet(this.wallet);
        
        // se nao tem saldo, nao mostra a popup
        if (this.walletBalances.isEmpty()) {
            this.addWarning(true, "wallet.no-balances");
            return;
        }

        // se tem saldos, ja pega o primeiro e sai mostrando
        this.selectedBalance = this.getWalletBalances().get(0);
        
        this.updateAndOpenDialog("balanceHistoryDialog", "dialogBalanceHistory");
    }
    
    /**
     * @return os tipos de carteira disponiveis para cadastro
     */
    public WalletType[] getAvailableWalletTypes() {
        return WalletType.values();
    }
}
